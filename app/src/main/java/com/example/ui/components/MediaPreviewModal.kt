package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DownloadProgress
import com.example.model.MediaResult
import com.example.model.QualityOption
import com.example.ui.MainUiState
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen

/**
 * Modern MediaPreviewModal with strict UI State lifecycle:
 * - ReadyToDownload: Displays media info and smart unified download button (NO download triggered)
 * - Downloading: Live progress tracking ("جاري حفظ 3 من 10...")
 * - Success: Displays gallery confirmation ("تم الحفظ في المعرض بنجاح!") ONLY after real file save
 * - Error: Displays failure notice with retry button
 */
@Composable
fun MediaPreviewModal(
    media: MediaResult,
    isArabic: Boolean,
    progress: DownloadProgress,
    uiState: MainUiState = MainUiState.ReadyToDownload(media),
    onDownloadQuality: (QualityOption?) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Robust multi-item count calculation across all possible extracted formats
    val totalUniqueItems = run {
        val set = mutableSetOf<String>()
        media.carouselItems.forEach { if (it.url.isNotBlank()) set.add(it.url) }
        media.photoUrls.forEach { if (it.isNotBlank()) set.add(it) }
        if (set.isEmpty() && media.videoUrl.isNotBlank()) set.add(media.videoUrl)
        set.size
    }

    val totalPhotos = run {
        val set = mutableSetOf<String>()
        media.photoUrls.forEach { if (it.isNotBlank()) set.add(it) }
        media.carouselItems.forEach {
            if (!it.format.equals("mp4", ignoreCase = true) && !it.url.contains(".mp4", ignoreCase = true)) {
                set.add(it.url)
            }
        }
        set.size
    }

    val totalVideos = run {
        val set = mutableSetOf<String>()
        media.carouselItems.forEach {
            if (it.format.equals("mp4", ignoreCase = true) || it.url.contains(".mp4", ignoreCase = true)) {
                set.add(it.url)
            }
        }
        if (set.isEmpty() && media.photoUrls.isEmpty() && media.videoUrl.isNotBlank()) {
            set.add(media.videoUrl)
        }
        set.size
    }

    val isBatch = totalUniqueItems > 1
    val isSinglePhoto = !isBatch && (totalPhotos == 1 || media.videoUrl.contains(".jpg", ignoreCase = true) || media.videoUrl.contains(".png", ignoreCase = true))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("media_preview_modal")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Platform Tag & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue.copy(alpha = 0.25f))
                        .border(1.dp, ElectricBlue, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = media.platform.uppercase(),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("close_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CyberTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Info Row (Poster thumbnail + Title + Details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or media placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbUrl = media.thumbnailUrl
                        ?: media.carouselItems.firstOrNull()?.url
                        ?: media.photoUrls.firstOrNull()

                    if (!thumbUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = "Thumbnail",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isSinglePhoto) Icons.Default.Image else Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Multi-item badge on thumbnail
                    if (isBatch) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$totalUniqueItems",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title.ifBlank { if (isArabic) "وسائط بدون عنوان" else "Untitled Media" },
                        color = CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!media.author.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "@${media.author}",
                            color = CyberTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBatch) {
                            if (isArabic) "$totalUniqueItems عناصر جاهزة للحفظ في المعرض" else "$totalUniqueItems items ready for Gallery"
                        } else {
                            if (isArabic) "جاهز للحفظ في ألبوم الهاتف" else "Ready to save to Gallery"
                        },
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Strictly separated UI States:
            // 1. Success -> only after actual completion
            // 2. Downloading -> during active network stream
            // 3. Error -> on failure
            // 4. ReadyToDownload -> only preview + button, NO DOWNLOAD TRIGGERED
            when (uiState) {
                // 1. SUCCESS STATE (Activated ONLY after real MediaStore save)
                is MainUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F261C))
                            .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isArabic) "تم الحفظ في المعرض بنجاح!" else "Saved to Gallery Successfully!",
                            color = SuccessGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.message,
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = Color(0xFF141417)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("dismiss_success_button")
                        ) {
                            Text(
                                text = if (isArabic) "تم (إغلاق)" else "Done (Close)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 2. DOWNLOADING STATE
                is MainUiState.Downloading -> {
                    val currentProg = uiState.progress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberBackground)
                            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeText = when {
                                currentProg.batchTotal > 1 -> {
                                    if (isArabic) {
                                        "جاري حفظ ${currentProg.batchCurrent} من ${currentProg.batchTotal}..."
                                    } else {
                                        "Saving ${currentProg.batchCurrent} of ${currentProg.batchTotal}..."
                                    }
                                }
                                currentProg.statusMessage.isNotBlank() -> currentProg.statusMessage
                                else -> if (isArabic) "جارٍ التحميل والحفظ..." else "Downloading to Gallery..."
                            }

                            Text(
                                text = activeText,
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentProg.progressPercent}%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (currentProg.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = NeonCyan,
                            trackColor = Color(0xFF1E2A40)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentProg.speedText.isNotBlank()) "Speed: ${currentProg.speedText}" else "Processing...",
                                color = CyberTextMuted,
                                fontSize = 11.sp
                            )

                            Text(
                                text = if (isArabic) "إلغاء التحميل" else "Cancel Download",
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onCancelDownload() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // 3. ERROR STATE
                is MainUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A1515))
                            .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage.ifBlank { if (isArabic) "حدث خطأ أثناء التحميل" else "Download error" },
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onDownloadQuality(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color(0xFF141417)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إعادة المحاولة" else "Retry Download",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 4. READY TO DOWNLOAD (Default): Unified Smart Button ONLY.
                // Absolutely NO auto-download; button click triggers onDownloadQuality(null)
                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val primaryTitle = when {
                            isBatch -> {
                                if (isArabic) "تحميل الكل ($totalUniqueItems ملفات)" else "Download All ($totalUniqueItems Files)"
                            }
                            isSinglePhoto -> {
                                if (isArabic) "تحميل الصورة (JPG)" else "Download Photo (JPG)"
                            }
                            else -> {
                                if (isArabic) "تحميل الفيديو (HD MP4)" else "Download Video (HD MP4)"
                            }
                        }

                        val primarySubtext = when {
                            isBatch && totalVideos > 0 && totalPhotos > 0 -> {
                                if (isArabic) "$totalPhotos صور (Pictures) • $totalVideos فيديوهات (Movies)"
                                else "$totalPhotos Photos (Pictures) • $totalVideos Videos (Movies)"
                            }
                            isBatch && totalPhotos > 0 -> {
                                if (isArabic) "$totalPhotos صور أصلية في ألبوم Pictures"
                                else "$totalPhotos Photos to Pictures Album"
                            }
                            isBatch && totalVideos > 0 -> {
                                if (isArabic) "$totalVideos فيديوهات عالية الدقة في ألبوم Movies"
                                else "$totalVideos Videos to Movies Album"
                            }
                            isSinglePhoto -> {
                                if (isArabic) "حفظ كصورة أصلية في ألبوم Pictures"
                                else "Save as High-Res Image in Pictures"
                            }
                            else -> {
                                if (isArabic) "حفظ كفيديو عالي الدقة في ألبوم Movies"
                                else "Save as HD MP4 Video in Movies"
                            }
                        }

                        // Smart Unified Action Button with direct lambda trigger
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, Color(0xFF00B4D8), ElectricBlue)
                                    )
                                )
                                .clickable {
                                    // Strictly triggered by user click, never invoked automatically
                                    onDownloadQuality(null)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("unified_download_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF141417).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isBatch -> Icons.Default.PhotoLibrary
                                                isSinglePhoto -> Icons.Default.Image
                                                else -> Icons.Default.Videocam
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF141417),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = primaryTitle,
                                            color = Color(0xFF141417),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = primarySubtext,
                                            color = Color(0xFF141417).copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF141417)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Secondary Option: Audio Only (MP3) if audio stream is available
                        if (!media.audioUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onDownloadQuality(
                                            QualityOption(
                                                label = "Audio Only",
                                                url = media.audioUrl,
                                                format = "mp3"
                                            )
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("download_audio_only_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = "Audio",
                                            tint = Color(0xFFFFB703),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isArabic) "تحميل الصوت فقط (MP3)" else "Download Audio Only (MP3)",
                                                color = CyberTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isArabic) "استخراج المقطع الصوتي إلى مجلد Music" else "Extract audio stream to Music folder",
                                                color = CyberTextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFB703).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFFFFB703), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "MP3",
                                            color = Color(0xFFFFB703),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
