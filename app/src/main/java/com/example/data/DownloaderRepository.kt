package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class DetectedMediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

data class SingleDownloadResult(
    val file: File?,
    val uri: Uri?,
    val mimeType: String,
    val bytesDownloaded: Long,
    val fileName: String,
    val mediaType: DetectedMediaType
)

data class BatchDownloadResult(
    val successCount: Int,
    val failedCount: Int,
    val totalCount: Int,
    val results: List<SingleDownloadResult>
)

data class DownloadQueueItem(
    val url: String,
    val formatHint: String? = null,
    val titlePrefix: String = "Media"
)

/**
 * Production-ready MediaStore Downloader Repository.
 *
 * Guarantees 100% strict separation between Images, Videos, and Audio:
 * - Images (JPG/PNG/WebP) -> DIRECTORY_PICTURES/MediaDownloader + MediaStore.Images.Media
 * - Videos (MP4) -> DIRECTORY_MOVIES/MediaDownloader + MediaStore.Video.Media
 * - Audio (MP3) -> DIRECTORY_MUSIC/MediaDownloader + MediaStore.Audio.Media
 *
 * Operates purely with Scoped Storage (API 29+ IS_PENDING flow) and legacy fallback.
 */
class DownloaderRepository(private val context: Context) {

    companion object {
        private const val TAG = "DownloaderRepository"
        private val COUNTER = AtomicLong(0L)
        private const val DIRECTORY_NAME = "MediaDownloader"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Determines whether a given media stream is an Image, Video, or Audio using
     * HTTP response headers, URL signatures, and format hints.
     */
    fun detectMediaType(
        url: String,
        contentType: String?,
        formatHint: String?
    ): DetectedMediaType {
        val cleanCt = (contentType ?: "").lowercase(Locale.ROOT)
        val cleanUrl = url.lowercase(Locale.ROOT)
        val cleanHint = (formatHint ?: "").lowercase(Locale.ROOT)

        // 1. Most authoritative: Real HTTP Content-Type header from server
        if (cleanCt.startsWith("image/") || cleanCt.contains("image")) {
            return DetectedMediaType.IMAGE
        }
        if (cleanCt.startsWith("video/") || cleanCt.contains("video")) {
            return DetectedMediaType.VIDEO
        }
        if (cleanCt.startsWith("audio/") || cleanCt.contains("audio")) {
            return DetectedMediaType.AUDIO
        }

        // 2. Format hint provided by server response / caller
        if (cleanHint in listOf("jpg", "jpeg", "png", "webp", "photo", "image", "album")) {
            return DetectedMediaType.IMAGE
        }
        if (cleanHint in listOf("mp4", "video", "reel", "clip", "mov")) {
            return DetectedMediaType.VIDEO
        }
        if (cleanHint in listOf("mp3", "audio", "music", "m4a", "aac")) {
            return DetectedMediaType.AUDIO
        }

        // 3. URL path and extension check
        val strippedUrl = cleanUrl.substringBefore("?")
        if (strippedUrl.endsWith(".jpg") || strippedUrl.endsWith(".jpeg") ||
            strippedUrl.endsWith(".png") || strippedUrl.endsWith(".webp") ||
            strippedUrl.endsWith(".heic") || strippedUrl.endsWith(".gif")
        ) {
            return DetectedMediaType.IMAGE
        }
        if (strippedUrl.endsWith(".mp4") || strippedUrl.endsWith(".mov") ||
            strippedUrl.endsWith(".webm") || strippedUrl.endsWith(".mkv")
        ) {
            return DetectedMediaType.VIDEO
        }
        if (strippedUrl.endsWith(".mp3") || strippedUrl.endsWith(".m4a") ||
            strippedUrl.endsWith(".aac") || strippedUrl.endsWith(".ogg")
        ) {
            return DetectedMediaType.AUDIO
        }

        // 4. Heuristic patterns for social media CDN links
        if (cleanUrl.contains("/photo/") || cleanUrl.contains("photo") ||
            cleanUrl.contains("img") || cleanUrl.contains("pic")
        ) {
            return DetectedMediaType.IMAGE
        }

        return DetectedMediaType.VIDEO
    }

    /**
     * Downloads a single media stream and saves it accurately into Android MediaStore.
     */
    suspend fun downloadMedia(
        downloadUrl: String,
        baseTitle: String,
        formatHint: String? = null,
        onProgress: (DownloadProgress) -> Unit = {}
    ): SingleDownloadResult = withContext(Dispatchers.IO) {
        val sanitizedBase = sanitizeFilename(baseTitle)
        val uniqueSuffix = "${System.currentTimeMillis()}_${COUNTER.incrementAndGet()}"

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            val msg = if (e.message?.contains("Unable to resolve host") == true) {
                "انقطع الاتصال بالإنترنت أو تعذر الوصول إلى الخادم"
            } else {
                "فشل الاتصال برابط التحميل: ${e.message}"
            }
            throw IOException(msg, e)
        }

        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            val errorMsg = when (code) {
                403 -> "انتهت صلاحية رابط التحميل أو يتطلب مصادقة (رمز 403)"
                404 -> "الملف غير موجود على خوادم المصدر (رمز 404)"
                429 -> "تم تجاوز حد الطلبات المسموح به من المنصة (رمز 429)"
                else -> "فشل تحميل الملف من السيرفر (رمز خطأ: $code)"
            }
            throw IOException(errorMsg)
        }

        val body = response.body ?: throw IOException("استجابة خادم التحميل فارغة")
        val rawContentType = response.header("Content-Type") ?: body.contentType()?.toString()
        val totalBytes = body.contentLength()

        // Sniff real media type (Image vs Video vs Audio)
        val mediaType = detectMediaType(downloadUrl, rawContentType, formatHint)

        val mimeType: String
        val extension: String
        val relativeFolder: String

        when (mediaType) {
            DetectedMediaType.IMAGE -> {
                mimeType = when {
                    rawContentType?.contains("png", ignoreCase = true) == true -> "image/png"
                    rawContentType?.contains("webp", ignoreCase = true) == true -> "image/webp"
                    else -> "image/jpeg"
                }
                extension = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                relativeFolder = Environment.DIRECTORY_PICTURES + "/" + DIRECTORY_NAME
            }
            DetectedMediaType.VIDEO -> {
                mimeType = "video/mp4"
                extension = "mp4"
                relativeFolder = Environment.DIRECTORY_MOVIES + "/" + DIRECTORY_NAME
            }
            DetectedMediaType.AUDIO -> {
                mimeType = "audio/mpeg"
                extension = "mp3"
                relativeFolder = Environment.DIRECTORY_MUSIC + "/" + DIRECTORY_NAME
            }
        }

        val fileName = "${sanitizedBase}_${uniqueSuffix}.$extension"

        var savedUri: Uri? = null
        var savedFile: File? = null
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = when (mediaType) {
                    DetectedMediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    DetectedMediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    DetectedMediaType.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                savedUri = context.contentResolver.insert(collectionUri, contentValues)
                    ?: throw IOException("تعذر إنشاء مسار الحفظ في نظام الوسائط (MediaStore)")

                outputStream = context.contentResolver.openOutputStream(savedUri)
                    ?: throw IOException("تعذر فتح تيار الكتابة في الذاكرة")
            } else {
                val publicDir = when (mediaType) {
                    DetectedMediaType.IMAGE -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    DetectedMediaType.VIDEO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    DetectedMediaType.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                }
                val targetDir = File(publicDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
                savedFile = File(targetDir, fileName)
                outputStream = FileOutputStream(savedFile)
            }

            val buffer = ByteArray(16 * 1024)
            var bytesCopied = 0L
            var read: Int
            val startTime = System.currentTimeMillis()
            var lastUpdate = 0L
            val inputStream: InputStream = body.byteStream()

            outputStream.use { out ->
                inputStream.use { inStream ->
                    while (inStream.read(buffer).also { read = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            throw IOException("تم إلغاء التنزيل بواسطة المستخدم")
                        }
                        out.write(buffer, 0, read)
                        bytesCopied += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250 || bytesCopied == totalBytes) {
                            lastUpdate = now
                            val elapsedSec = (now - startTime) / 1000.0
                            val speed = if (elapsedSec > 0) bytesCopied / elapsedSec else 0.0
                            val speedText = formatSpeed(speed)
                            val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0

                            onProgress(
                                DownloadProgress(
                                    url = downloadUrl,
                                    fileName = fileName,
                                    bytesDownloaded = bytesCopied,
                                    totalBytes = totalBytes,
                                    progressPercent = percent,
                                    speedText = speedText,
                                    isDownloading = true
                                )
                            )
                        }
                    }
                    out.flush()
                }
            }

            // Publish completed entry to MediaStore (Scoped Storage API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && savedUri != null) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(savedUri, publishValues, null, null)
            }

            // Instant scan for local files
            if (savedFile != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(savedFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            SingleDownloadResult(
                file = savedFile,
                uri = savedUri,
                mimeType = mimeType,
                bytesDownloaded = bytesCopied,
                fileName = fileName,
                mediaType = mediaType
            )
        } catch (e: Exception) {
            // Clean up incomplete pending file if error occurred
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && savedUri != null) {
                try {
                    context.contentResolver.delete(savedUri, null, null)
                } catch (_: Exception) {}
            } else if (savedFile != null && savedFile.exists()) {
                try {
                    savedFile.delete()
                } catch (_: Exception) {}
            }
            throw e
        }
    }

    /**
     * Downloads a batch of media items (e.g. Instagram Carousel / TikTok Album) sequentially
     * with live item-by-item progress reporting.
     */
    suspend fun downloadBatch(
        items: List<DownloadQueueItem>,
        baseTitle: String,
        onBatchProgress: (currentIndex: Int, totalCount: Int, itemProgress: DownloadProgress) -> Unit
    ): BatchDownloadResult = withContext(Dispatchers.IO) {
        val total = items.size
        val results = mutableListOf<SingleDownloadResult>()
        var failed = 0

        items.forEachIndexed { index, queueItem ->
            val currentNumber = index + 1
            val itemTitle = "${baseTitle}_${queueItem.titlePrefix}_$currentNumber"

            onBatchProgress(
                currentNumber,
                total,
                DownloadProgress(
                    url = queueItem.url,
                    fileName = itemTitle,
                    progressPercent = 0,
                    isDownloading = true,
                    speedText = "$currentNumber / $total"
                )
            )

            try {
                val res = downloadMedia(
                    downloadUrl = queueItem.url,
                    baseTitle = itemTitle,
                    formatHint = queueItem.formatHint,
                    onProgress = { itemProgress ->
                        onBatchProgress(currentNumber, total, itemProgress)
                    }
                )
                results.add(res)
            } catch (e: Exception) {
                Log.e(TAG, "Failed item $currentNumber of $total: ${e.message}", e)
                failed++
            }
        }

        BatchDownloadResult(
            successCount = results.size,
            failedCount = failed,
            totalCount = total,
            results = results
        )
    }

    private fun sanitizeFilename(name: String): String {
        val clean = name.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
            .take(45)
            .trim('_')
        return if (clean.isBlank()) "Media" else clean
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024)
            else -> String.format(Locale.US, "%.0f B/s", bytesPerSec)
        }
    }
}
