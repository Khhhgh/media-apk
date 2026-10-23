package com.example.model

import androidx.annotation.Keep

@Keep
data class PlatformInfo(
    val id: String,
    val name: String,
    val nameAr: String = "",
    val enabled: Boolean = true,
    val maintenanceMessage: String? = null,
    val patterns: List<String> = emptyList(),
    val sampleUrl: String = "",
    val icon: String = "web",
    val badge: String = ""
) {
    fun getDisplayName(isArabic: Boolean): String {
        return if (isArabic && nameAr.isNotBlank()) nameAr else name
    }

    fun matchesUrl(url: String): Boolean {
        val lower = url.lowercase()
        return patterns.any { pat -> lower.contains(pat.lowercase()) }
    }
}

@Keep
data class PlatformsResponse(
    val success: Boolean = true,
    val version: Int = 1,
    val platforms: List<PlatformInfo> = emptyList()
)
