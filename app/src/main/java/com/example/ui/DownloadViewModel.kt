package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DetectedMediaType
import com.example.data.DownloadQueueItem
import com.example.data.DownloaderRepository
import com.example.model.DownloadItem
import com.example.model.DownloadProgress
import com.example.model.MediaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data class Downloading(
        val currentItem: Int,
        val totalItems: Int,
        val percent: Int,
        val speedText: String,
        val statusMessage: String
    ) : DownloadUiState
    data class Success(
        val message: String,
        val savedCount: Int,
        val totalCount: Int
    ) : DownloadUiState
    data class Error(val errorMessage: String) : DownloadUiState
}

/**
 * Modern, production-ready DownloadViewModel.
 *
 * Manages UI states, bounded coroutine downloading, live item counters,
 * and MediaStore persistence via DownloaderRepository.
 */
class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloaderRepository(application.applicationContext)
    private val downloadDao = AppDatabase.getInstance(application.applicationContext).downloadDao()

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var activeJob: Job? = null

    /**
     * Executes unified download with strict Scoped Storage routing.
     * Handles single videos, single photos, and multi-item carousels/albums.
     */
    fun startUnifiedDownload(media: MediaResult, isAudioOnly: Boolean = false, isArabic: Boolean = true) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            // 1. Audio-only path
            if (isAudioOnly && !media.audioUrl.isNullOrBlank()) {
                val statusMsg = if (isArabic) "جاري تحميل المقطع الصوتي..." else "Downloading audio..."
                _uiState.value = DownloadUiState.Downloading(1, 1, 0, "", statusMsg)
                _progress.value = DownloadProgress(isDownloading = true, statusMessage = statusMsg)

                try {
                    val result = repository.downloadMedia(
                        downloadUrl = media.audioUrl,
                        baseTitle = media.title,
                        formatHint = "mp3",
                        onProgress = { p ->
                            _progress.value = p
                            _uiState.value = DownloadUiState.Downloading(
                                currentItem = 1,
                                totalItems = 1,
                                percent = p.progressPercent,
                                speedText = p.speedText,
                                statusMessage = statusMsg
                            )
                        }
                    )

                    val savedPath = result.file?.absolutePath ?: (result.uri?.toString() ?: "")
                    val historyItem = DownloadItem(
                        title = media.title,
                        platform = media.platform,
                        sourceUrl = media.sourceUrl,
                        localFilePath = savedPath,
                        mediaUri = result.uri?.toString() ?: savedPath,
                        fileSize = result.bytesDownloaded,
                        formattedSize = formatBytes(result.bytesDownloaded),
                        mimeType = result.mimeType,
                        thumbnailUri = media.thumbnailUrl
                    )
                    downloadDao.insertDownload(historyItem)

                    val successMsg = if (isArabic) "تم حفظ الصوت في المعرض بنجاح!" else "Audio saved to Gallery!"
                    _uiState.value = DownloadUiState.Success(successMsg, 1, 1)
                    _progress.value = _progress.value.copy(isDownloading = false, isSuccess = true, statusMessage = successMsg)
                    _toastEvents.emit(successMsg)
                } catch (e: Exception) {
                    val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                    _uiState.value = DownloadUiState.Error(errorMsg)
                    _progress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                    _toastEvents.emit(errorMsg)
                }
                return@launch
            }

            // 2. Multi-item Carousel / Album path: Collect ALL unique items
            val queue = mutableListOf<DownloadQueueItem>()
            val seenUrls = mutableSetOf<String>()

            // A) Carousel items
            media.carouselItems.forEachIndexed { idx, it ->
                if (it.url.isNotBlank() && seenUrls.add(it.url)) {
                    val isVid = it.format.equals("mp4", ignoreCase = true) || it.url.contains(".mp4", ignoreCase = true)
                    queue.add(
                        DownloadQueueItem(
                            url = it.url,
                            formatHint = if (isVid) "mp4" else "jpg",
                            titlePrefix = if (isVid) "video_${idx + 1}" else "photo_${idx + 1}"
                        )
                    )
                }
            }

            // B) Photo URLs (e.g. Instagram/TikTok albums)
            media.photoUrls.forEachIndexed { idx, url ->
                if (url.isNotBlank() && seenUrls.add(url)) {
                    queue.add(
                        DownloadQueueItem(
                            url = url,
                            formatHint = "jpg",
                            titlePrefix = "photo_${queue.size + 1}"
                        )
                    )
                }
            }

            // C) Fallback to videoUrl if queue is still empty
            if (queue.isEmpty() && media.videoUrl.isNotBlank() && seenUrls.add(media.videoUrl)) {
                val isImage = media.videoUrl.contains(".jpg", ignoreCase = true) || media.videoUrl.contains(".png", ignoreCase = true)
                queue.add(
                    DownloadQueueItem(
                        url = media.videoUrl,
                        formatHint = if (isImage) "jpg" else "mp4",
                        titlePrefix = if (isImage) "photo" else "video"
                    )
                )
            }

            val totalCount = queue.size

            if (totalCount > 1) {
                val initStatus = if (isArabic) "جاري حفظ 1 من $totalCount..." else "Saving 1 of $totalCount..."
                _uiState.value = DownloadUiState.Downloading(1, totalCount, 0, "", initStatus)
                _progress.value = DownloadProgress(isDownloading = true, batchCurrent = 1, batchTotal = totalCount, statusMessage = initStatus)

                try {
                    val batchResult = repository.downloadBatch(
                        items = queue,
                        baseTitle = media.title
                    ) { current, total, itemProg ->
                        val currentStatus = if (isArabic) "جاري حفظ $current من $total..." else "Saving $current of $total..."
                        _progress.value = itemProg.copy(
                            batchCurrent = current,
                            batchTotal = total,
                            statusMessage = currentStatus
                        )
                        _uiState.value = DownloadUiState.Downloading(
                            currentItem = current,
                            totalItems = total,
                            percent = itemProg.progressPercent,
                            speedText = itemProg.speedText,
                            statusMessage = currentStatus
                        )
                    }

                    // Save history record
                    var firstPath = ""
                    var firstUri = ""
                    var totalBytes = 0L
                    var videoCount = 0
                    var imageCount = 0

                    batchResult.results.forEach { res ->
                        totalBytes += res.bytesDownloaded
                        val path = res.file?.absolutePath ?: (res.uri?.toString() ?: "")
                        if (firstPath.isBlank()) firstPath = path
                        if (firstUri.isBlank()) firstUri = res.uri?.toString() ?: path

                        if (res.mediaType == DetectedMediaType.VIDEO) {
                            videoCount++
                        } else {
                            imageCount++
                        }
                    }

                    val summaryItem = DownloadItem(
                        title = "${media.title} ($totalCount ملفات)",
                        platform = media.platform,
                        sourceUrl = media.sourceUrl,
                        localFilePath = firstPath,
                        mediaUri = firstUri,
                        fileSize = totalBytes,
                        formattedSize = "$videoCount فيديو • $imageCount صورة",
                        mimeType = if (videoCount > 0) "video/mp4" else "image/jpeg",
                        thumbnailUri = media.thumbnailUrl ?: media.carouselItems.firstOrNull()?.url ?: media.photoUrls.firstOrNull()
                    )
                    downloadDao.insertDownload(summaryItem)

                    val successCount = batchResult.successCount
                    val finalMsg = if (isArabic) {
                        "تم حفظ جميع العناصر بنجاح في المعرض ($successCount من $totalCount)"
                    } else {
                        "Saved all $successCount items to Gallery!"
                    }

                    _uiState.value = DownloadUiState.Success(finalMsg, successCount, totalCount)
                    _progress.value = DownloadProgress(
                        isDownloading = false,
                        isSuccess = true,
                        batchCurrent = totalCount,
                        batchTotal = totalCount,
                        statusMessage = finalMsg
                    )
                    _toastEvents.emit(finalMsg)
                } catch (e: Exception) {
                    val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                    _uiState.value = DownloadUiState.Error(errorMsg)
                    _progress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                    _toastEvents.emit(errorMsg)
                }
                return@launch
            }

            // 3. Single Item Path (Single photo or single video)
            val singleItem = queue.firstOrNull()
            val singleUrl = singleItem?.url ?: media.videoUrl
            val formatHint = singleItem?.formatHint

            val startMsg = if (isArabic) "جاري التحميل..." else "Downloading..."
            _uiState.value = DownloadUiState.Downloading(1, 1, 0, "", startMsg)
            _progress.value = DownloadProgress(isDownloading = true, statusMessage = startMsg)

            try {
                val result = repository.downloadMedia(
                    downloadUrl = singleUrl,
                    baseTitle = media.title,
                    formatHint = formatHint,
                    onProgress = { p ->
                        _progress.value = p
                        _uiState.value = DownloadUiState.Downloading(
                            currentItem = 1,
                            totalItems = 1,
                            percent = p.progressPercent,
                            speedText = p.speedText,
                            statusMessage = startMsg
                        )
                    }
                )

                val savedPath = result.file?.absolutePath ?: (result.uri?.toString() ?: "")
                val historyItem = DownloadItem(
                    title = media.title,
                    platform = media.platform,
                    sourceUrl = media.sourceUrl,
                    localFilePath = savedPath,
                    mediaUri = result.uri?.toString() ?: savedPath,
                    fileSize = result.bytesDownloaded,
                    formattedSize = formatBytes(result.bytesDownloaded),
                    mimeType = result.mimeType,
                    thumbnailUri = media.thumbnailUrl
                )
                downloadDao.insertDownload(historyItem)

                val successMsg = if (isArabic) {
                    if (result.mediaType == DetectedMediaType.IMAGE) "تم حفظ الصورة في المعرض بنجاح!"
                    else "تم حفظ الفيديو في المعرض بنجاح!"
                } else {
                    if (result.mediaType == DetectedMediaType.IMAGE) "Photo saved to Gallery successfully!"
                    else "Video saved to Gallery successfully!"
                }

                _uiState.value = DownloadUiState.Success(successMsg, 1, 1)
                _progress.value = _progress.value.copy(isDownloading = false, isSuccess = true, statusMessage = successMsg)
                _toastEvents.emit(successMsg)
            } catch (e: Exception) {
                val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                _uiState.value = DownloadUiState.Error(errorMsg)
                _progress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                _toastEvents.emit(errorMsg)
            }
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        _uiState.value = DownloadUiState.Idle
        _progress.value = DownloadProgress()
    }

    fun resetState() {
        _uiState.value = DownloadUiState.Idle
        _progress.value = DownloadProgress()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
