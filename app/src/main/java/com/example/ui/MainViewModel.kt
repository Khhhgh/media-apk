package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdManager
import com.example.data.AppDatabase
import com.example.data.DetectedMediaType
import com.example.data.DownloadQueueItem
import com.example.data.DownloaderRepository
import com.example.data.api.ApiClient
import com.example.model.DownloadItem
import com.example.model.DownloadProgress
import com.example.model.MediaResult
import com.example.model.PlatformInfo
import com.example.model.QualityOption
import com.example.service.MasterDownloadEngine
import com.example.service.UrlResolver
import com.example.service.YouTubeBlockedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Extracting : MainUiState
    data class ReadyToDownload(val media: MediaResult) : MainUiState
    data class Downloading(
        val media: MediaResult,
        val progress: DownloadProgress
    ) : MainUiState
    data class Success(
        val media: MediaResult,
        val message: String,
        val savedCount: Int,
        val totalCount: Int
    ) : MainUiState
    data class Error(
        val errorMessage: String,
        val media: MediaResult? = null
    ) : MainUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext

    private val apiClient = ApiClient()
    private val downloadEngine = MasterDownloadEngine(context = context, apiClient = apiClient)
    private val downloaderRepository = DownloaderRepository(context)
    private val downloadDao = AppDatabase.getInstance(context).downloadDao()
    private val adManager = AdManager(context)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Dynamic platforms fetched from Ubuntu VPS: GET /api/platforms
    private val _platforms = MutableStateFlow<List<PlatformInfo>>(ApiClient.DEFAULT_PLATFORMS)
    val platforms: StateFlow<List<PlatformInfo>> = _platforms.asStateFlow()

    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    val downloadsHistory: StateFlow<List<DownloadItem>> = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _detectedPlatform = MutableStateFlow<PlatformInfo?>(null)
    val detectedPlatform: StateFlow<PlatformInfo?> = _detectedPlatform.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractedMedia = MutableStateFlow<MediaResult?>(null)
    val extractedMedia: StateFlow<MediaResult?> = _extractedMedia.asStateFlow()

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _isArabic = MutableStateFlow(true)
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    private val _showYouTubeBlockedDialog = MutableStateFlow(false)
    val showYouTubeBlockedDialog: StateFlow<Boolean> = _showYouTubeBlockedDialog.asStateFlow()

    private val _showInAppAd = MutableStateFlow(false)
    val showInAppAd: StateFlow<Boolean> = _showInAppAd.asStateFlow()

    fun dismissInAppAd() {
        _showInAppAd.value = false
    }

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    private val _activePlayingItem = MutableStateFlow<DownloadItem?>(null)
    val activePlayingItem: StateFlow<DownloadItem?> = _activePlayingItem.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Downloader, 1: History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var currentDownloadJob: Job? = null

    init {
        loadDynamicPlatforms()
        checkClipboardForLink()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Loads live dynamic platform status from the Ubuntu VPS.
     * When new platforms are added or toggled in platforms.json, they appear immediately.
     */
    fun loadDynamicPlatforms() {
        viewModelScope.launch {
            try {
                val list = apiClient.fetchPlatforms()
                if (list.isNotEmpty()) {
                    _platforms.value = list
                    _isServerOnline.value = true
                    updateDetectedPlatform(_urlInput.value)
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Could not fetch dynamic platforms: ${e.message}")
                _isServerOnline.value = false
            }
        }
    }

    private fun updateDetectedPlatform(input: String) {
        if (input.isBlank()) {
            _detectedPlatform.value = null
            return
        }
        val match = _platforms.value.firstOrNull { it.matchesUrl(input) }
        _detectedPlatform.value = match
    }

    fun onUrlChange(newUrl: String) {
        val hasHttp = newUrl.contains("http://", ignoreCase = true) || newUrl.contains("https://", ignoreCase = true)
        val finalUrl = if (hasHttp) {
            val sanitized = UrlResolver.sanitizeAndExtractUrl(newUrl)
            if (sanitized.isNotBlank()) sanitized else newUrl
        } else {
            newUrl
        }

        _urlInput.value = finalUrl
        updateDetectedPlatform(finalUrl)

        if (downloadEngine.isYouTubeUrl(finalUrl)) {
            _showYouTubeBlockedDialog.value = true
        }
    }

    fun dismissYouTubeDialog() {
        _showYouTubeBlockedDialog.value = false
        _urlInput.value = ""
        _detectedPlatform.value = null
    }

    fun toggleLanguage() {
        _isArabic.value = !_isArabic.value
    }

    fun checkClipboardForLink() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) {
                    val extracted = UrlResolver.sanitizeAndExtractUrl(text)
                    if (extracted.startsWith("http://") || extracted.startsWith("https://")) {
                        if (downloadEngine.isYouTubeUrl(extracted)) {
                            _clipboardDetectedUrl.value = null
                        } else if (extracted != _urlInput.value) {
                            _clipboardDetectedUrl.value = extracted
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Clipboard inspection: ${e.message}")
        }
    }

    fun pasteAndDownload(urlToUse: String? = null) {
        val raw = urlToUse ?: run {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.text?.toString()
            } else null
        } ?: return

        val target = UrlResolver.sanitizeAndExtractUrl(raw)
        if (target.isNotBlank()) {
            _urlInput.value = target
            updateDetectedPlatform(target)
            _clipboardDetectedUrl.value = null
            startExtraction(target)
        }
    }

    fun startExtraction(url: String = _urlInput.value) {
        val cleanUrl = UrlResolver.sanitizeAndExtractUrl(url)
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            emitToast(if (_isArabic.value) "يرجى إدخال رابط صالح" else "Please enter a valid link")
            return
        }
        _urlInput.value = cleanUrl
        updateDetectedPlatform(cleanUrl)

        if (downloadEngine.isYouTubeUrl(cleanUrl)) {
            _showYouTubeBlockedDialog.value = true
            return
        }

        // عند الضغط على ابدا ثالث مره يظهر الإعلان داخل التطبيق
        if (adManager.shouldShowInAppAd()) {
            _showInAppAd.value = true
        }

        viewModelScope.launch {
            _isExtracting.value = true
            _extractedMedia.value = null
            _downloadProgress.value = DownloadProgress()
            _uiState.value = MainUiState.Extracting

            try {
                val result = downloadEngine.resolveMedia(cleanUrl, _platforms.value)
                _extractedMedia.value = result
                _downloadProgress.value = DownloadProgress()
                _uiState.value = MainUiState.ReadyToDownload(result)
                emitToast(if (_isArabic.value) "تم استخراج الميديا بنجاح! جاهز للتنزيل" else "Media extracted successfully! Ready to download")
            } catch (e: YouTubeBlockedException) {
                _uiState.value = MainUiState.Idle
                _showYouTubeBlockedDialog.value = true
            } catch (e: Exception) {
                Log.e("MainViewModel", "Extraction failed: ${e.message}", e)
                val reason = (e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName).take(300)
                val msg = if (_isArabic.value) "فشل الاستخراج: $reason" else "Extraction failed: $reason"
                _uiState.value = MainUiState.Error(msg)
                emitToast(msg)
            } finally {
                _isExtracting.value = false
            }
        }
    }

    fun startDownload(quality: QualityOption? = null) {
        val media = _extractedMedia.value ?: return
        val isAudio = quality?.format == "mp3"
        startUnifiedDownload(media, isAudioOnly = isAudio)
    }

    /**
     * Unified download executing on Dispatchers.IO with strict separation between
     * Photos (MediaStore.Images), Videos (MediaStore.Video), and Audio (MediaStore.Audio).
     *
     * State lifecycle:
     * - Idle -> Extracting -> ReadyToDownload -> Downloading -> Success / Error
     */
    fun startUnifiedDownload(media: MediaResult, isAudioOnly: Boolean = false) {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val isArabic = _isArabic.value

            // 1. Audio-only path
            if (isAudioOnly && !media.audioUrl.isNullOrBlank()) {
                val statusMsg = if (isArabic) "جاري تحميل المقطع الصوتي..." else "Downloading audio..."
                val initProg = DownloadProgress(
                    url = media.audioUrl,
                    fileName = "${media.title}.mp3",
                    isDownloading = true,
                    statusMessage = statusMsg
                )
                _downloadProgress.value = initProg
                _uiState.value = MainUiState.Downloading(media, initProg)

                try {
                    val result = downloaderRepository.downloadMedia(
                        downloadUrl = media.audioUrl,
                        baseTitle = media.title,
                        formatHint = "mp3",
                        onProgress = { p ->
                            _downloadProgress.value = p
                            _uiState.value = MainUiState.Downloading(media, p)
                        }
                    )

                    val savedPath = result.file?.absolutePath ?: (result.uri?.toString() ?: "")
                    val item = DownloadItem(
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
                    downloadDao.insertDownload(item)

                    val successMsg = if (isArabic) "تم حفظ الصوت في المعرض بنجاح!" else "Audio saved to Gallery!"
                    val finalProg = _downloadProgress.value.copy(
                        isDownloading = false,
                        isSuccess = true,
                        statusMessage = successMsg
                    )
                    _downloadProgress.value = finalProg
                    _uiState.value = MainUiState.Success(media, successMsg, 1, 1)
                    emitToast(successMsg)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Audio download error", e)
                    val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                    _downloadProgress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                    _uiState.value = MainUiState.Error(errorMsg, media)
                    emitToast(errorMsg)
                }
                return@launch
            }

            // 2. Multi-item Carousel / Album path: Collect ALL unique items from carouselItems, photoUrls, and videoUrl
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

            // B) Photo URLs (e.g. from Instagram / TikTok albums)
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

            val total = queue.size

            if (total > 1) {
                // Batch download of ALL items
                val initStatus = if (isArabic) "جاري حفظ 1 من $total..." else "Saving 1 of $total..."
                val initProg = DownloadProgress(
                    url = queue.first().url,
                    fileName = media.title,
                    isDownloading = true,
                    batchTotal = total,
                    batchCurrent = 1,
                    statusMessage = initStatus
                )
                _downloadProgress.value = initProg
                _uiState.value = MainUiState.Downloading(media, initProg)

                try {
                    val batchResult = downloaderRepository.downloadBatch(
                        items = queue,
                        baseTitle = media.title
                    ) { current, count, itemProg ->
                        val currentStatus = if (isArabic) "جاري حفظ $current من $count..." else "Saving $current of $count..."
                        val updatedProg = itemProg.copy(
                            batchCurrent = current,
                            batchTotal = count,
                            statusMessage = currentStatus
                        )
                        _downloadProgress.value = updatedProg
                        _uiState.value = MainUiState.Downloading(media, updatedProg)
                    }

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
                        title = "${media.title} ($total ملفات)",
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
                        "تم حفظ جميع العناصر بنجاح في المعرض ($successCount من $total)"
                    } else {
                        "Saved all $successCount items to Gallery!"
                    }

                    val finalProg = DownloadProgress(
                        url = queue.first().url,
                        fileName = media.title,
                        isDownloading = false,
                        isSuccess = true,
                        batchCurrent = total,
                        batchTotal = total,
                        statusMessage = finalMsg
                    )
                    _downloadProgress.value = finalProg
                    _uiState.value = MainUiState.Success(media, finalMsg, successCount, total)
                    emitToast(finalMsg)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Carousel batch download error", e)
                    val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                    _downloadProgress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                    _uiState.value = MainUiState.Error(errorMsg, media)
                    emitToast(errorMsg)
                }
                return@launch
            }

            // 3. Single Item Path (Single photo or single video)
            val singleItem = queue.firstOrNull()
            val singleUrl = singleItem?.url ?: media.videoUrl
            val formatHint = singleItem?.formatHint

            val startMsg = if (isArabic) "جاري التحميل..." else "Downloading..."
            val initProg = DownloadProgress(
                url = singleUrl,
                fileName = media.title,
                isDownloading = true,
                statusMessage = startMsg
            )
            _downloadProgress.value = initProg
            _uiState.value = MainUiState.Downloading(media, initProg)

            try {
                val result = downloaderRepository.downloadMedia(
                    downloadUrl = singleUrl,
                    baseTitle = media.title,
                    formatHint = formatHint,
                    onProgress = { p ->
                        _downloadProgress.value = p
                        _uiState.value = MainUiState.Downloading(media, p)
                    }
                )

                val savedPath = result.file?.absolutePath ?: (result.uri?.toString() ?: "")
                val item = DownloadItem(
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
                downloadDao.insertDownload(item)

                val successMsg = if (isArabic) {
                    if (result.mediaType == DetectedMediaType.IMAGE) "تم حفظ الصورة في المعرض بنجاح!"
                    else "تم حفظ الفيديو في المعرض بنجاح!"
                } else {
                    if (result.mediaType == DetectedMediaType.IMAGE) "Photo saved to Gallery successfully!"
                    else "Video saved to Gallery successfully!"
                }

                val finalProg = _downloadProgress.value.copy(
                    isDownloading = false,
                    isSuccess = true,
                    statusMessage = successMsg
                )
                _downloadProgress.value = finalProg
                _uiState.value = MainUiState.Success(media, successMsg, 1, 1)
                emitToast(successMsg)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Single download error", e)
                val errorMsg = e.message ?: (if (isArabic) "فشل التحميل" else "Download failed")
                _downloadProgress.value = DownloadProgress(isError = true, errorMessage = errorMsg)
                _uiState.value = MainUiState.Error(errorMsg, media)
                emitToast(errorMsg)
            }
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _downloadProgress.value = DownloadProgress(isDownloading = false)
        val currentMedia = _extractedMedia.value
        if (currentMedia != null) {
            _uiState.value = MainUiState.ReadyToDownload(currentMedia)
        } else {
            _uiState.value = MainUiState.Idle
        }
        emitToast(if (_isArabic.value) "تم إلغاء التحميل" else "Download cancelled")
    }

    fun dismissExtractedModal() {
        _extractedMedia.value = null
        _downloadProgress.value = DownloadProgress()
        _uiState.value = MainUiState.Idle
    }

    fun deleteHistoryItem(item: DownloadItem) {
        viewModelScope.launch {
            downloadDao.deleteDownload(item)
            try {
                if (item.localFilePath.isNotBlank()) {
                    val file = File(item.localFilePath)
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Could not delete local file: ${e.message}")
            }
            emitToast(if (_isArabic.value) "تم حذف العنصر" else "Item deleted")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            downloadDao.clearAll()
            emitToast(if (_isArabic.value) "تم مسح السجل" else "History cleared")
        }
    }

    fun playItem(item: DownloadItem) {
        _activePlayingItem.value = item
    }

    fun dismissPlayer() {
        _activePlayingItem.value = null
    }

    fun shareItem(item: DownloadItem) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                if (item.mediaUri.startsWith("content://")) {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(item.mediaUri))
                } else {
                    putExtra(Intent.EXTRA_TEXT, item.sourceUrl)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            emitToast("Share failed: ${e.message}")
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvents.emit(msg)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
