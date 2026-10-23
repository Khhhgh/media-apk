package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.model.MediaResult
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
 * Modern Compose DownloadScreen component.
 *
 * Provides a unified smart action button for saving media (single or multi-item batch)
 * with strict Scoped Storage routing:
 * - Images -> MediaStore.Images.Media / DIRECTORY_PICTURES
 * - Videos -> MediaStore.Video.Media / DIRECTORY_MOVIES
 * - Audio  -> MediaStore.Audio.Media / DIRECTORY_MUSIC
 */
@Composable
fun DownloadScreen(
    media: MediaResult,
    viewModel: DownloadViewModel,
    isArabic: Boolean = true,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val hasCarousel = media.carouselItems.isNotEmpty()
    val isMultiPhoto = media.photoUrls.size > 1
    val isBatch = hasCarousel || isMultiPhoto

    val totalItems = when {
        hasCarousel -> media.carouselItems.size
        isMultiPhoto -> media.photoUrls.size
        else -> 1
    }

    val videoCount = when {
        hasCarousel -> media.carouselItems.count { it.format.equals("mp4", ignoreCase = true) }
        media.photoUrls.isEmpty() && media.videoUrl.isNotBlank() -> 1
        else -> 0
    }

    val photoCount = when {
        hasCarousel -> media.carouselItems.count { !it.format.equals("mp4", ignoreCase = true) }
        isMultiPhoto -> media.photoUrls.size
        media.photoUrls.size == 1 -> 1
        else -> 0
    }

    val isSinglePhoto = !isBatch && (media.photoUrls.size == 1 || (!media.videoUrl.contains(".mp4") && media.videoUrl.contains(".jpg")))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("download_screen_card")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(1.dp, ElectricBlue, RoundedCornerShape(10.dp))
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = CyberTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thumbnail and media details
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
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
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (isBatch) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalItems",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${media.author}",
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBatch) {
                        if (isArabic) "$totalItems عناصر جاهزة للتنزيل المباشر" else "$totalItems items ready to save"
                    } else {
                        if (isArabic) "جاهز للحفظ في المعرض" else "Ready to save to Gallery"
                    },
                    color = SuccessGreen,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UI State Handling
        when (val state = uiState) {
            is DownloadUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F261C))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.resetState(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color(0xFF141417)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إغلاق" else "Done",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            is DownloadUiState.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.statusMessage,
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.percent}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (state.percent / 100f).coerceIn(0f, 1f) },
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
                            text = if (state.speedText.isNotBlank()) "Speed: ${state.speedText}" else "Processing...",
                            color = CyberTextMuted,
                            fontSize = 11.sp
                        )

                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.cancelDownload() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            is DownloadUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A1515))
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        color = ErrorRed,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.startUnifiedDownload(media, isArabic = isArabic) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color(0xFF141417)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "إعادة المحاولة" else "Retry")
                    }
                }
            }

            DownloadUiState.Idle -> {
                // Unified Action Button
                val primaryTitle = when {
                    isBatch -> if (isArabic) "تحميل الكل ($totalItems ملفات)" else "Download All ($totalItems Files)"
                    isSinglePhoto -> if (isArabic) "تحميل الصورة (JPG)" else "Download Photo (JPG)"
                    else -> if (isArabic) "تحميل الفيديو (HD MP4)" else "Download Video (HD MP4)"
                }

                val primarySubtext = when {
                    isBatch && videoCount > 0 && photoCount > 0 -> {
                        if (isArabic) "$photoCount صور (Pictures) • $videoCount فيديوهات (Movies)"
                        else "$photoCount Photos • $videoCount Videos"
                    }
                    isBatch && photoCount > 0 -> {
                        if (isArabic) "$photoCount صور أصلية في ألبوم Pictures" else "$photoCount Photos to Pictures"
                    }
                    isBatch && videoCount > 0 -> {
                        if (isArabic) "$videoCount فيديوهات عالية الدقة في ألبوم Movies" else "$videoCount Videos to Movies"
                    }
                    isSinglePhoto -> {
                        if (isArabic) "حفظ كصورة أصلية في ألبوم Pictures" else "Save High-Res Image to Pictures"
                    }
                    else -> {
                        if (isArabic) "حفظ كفيديو عالي الدقة في ألبوم Movies" else "Save HD Video to Movies"
                    }
                }

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
                            viewModel.startUnifiedDownload(media, isAudioOnly = false, isArabic = isArabic)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("download_screen_unified_button")
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
                                    .size(40.dp)
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
                                    modifier = Modifier.size(22.dp)
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
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFF141417),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (!media.audioUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.startUnifiedDownload(media, isAudioOnly = true, isArabic = isArabic)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                Text(
                                    text = if (isArabic) "تحميل الصوت فقط (MP3)" else "Audio Only (MP3)",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
