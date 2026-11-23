package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val groupName: String,
    val logo: String = "",
    val backdrop: String = "",
    val url: String = "",
    val url2: String = "",
    val url3: String = "",
    val referer: String = "",
    val userAgent: String = "",
    val url2Name: String = "",
    val url2Referer: String = "",
    val url2UserAgent: String = "",
    val url3Name: String = "",
    val url3Referer: String = "",
    val url3UserAgent: String = "",
    val drmScheme: String = "",
    val drmLicense: String = "",
    val url2DrmScheme: String = "",
    val url2DrmLicense: String = "",
    val url3DrmScheme: String = "",
    val url3DrmLicense: String = "",
    val type: String = "live",
    val featured: Boolean = false,
    val featuredOrder: Int = 0,
    val orderIndex: Int = 0,
    val enabled: Boolean = true,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun availableStreams(): List<StreamSource> {
        val list = mutableListOf<StreamSource>()
        if (url.isNotBlank()) {
            list.add(
                StreamSource(
                    index = 1,
                    label = "",
                    rawUrl = url.trim(),
                    referer = referer,
                    userAgent = userAgent,
                    drmScheme = drmScheme,
                    drmLicense = drmLicense,
                )
            )
        }
        if (url2.isNotBlank()) {
            list.add(
                StreamSource(
                    index = 2,
                    label = url2Name,
                    rawUrl = url2.trim(),
                    referer = url2Referer.ifBlank { referer },
                    userAgent = url2UserAgent.ifBlank { userAgent },
                    drmScheme = url2DrmScheme.ifBlank { drmScheme },
                    drmLicense = url2DrmLicense.ifBlank { drmLicense },
                )
            )
        }
        if (url3.isNotBlank()) {
            list.add(
                StreamSource(
                    index = 3,
                    label = url3Name,
                    rawUrl = url3.trim(),
                    referer = url3Referer.ifBlank { referer },
                    userAgent = url3UserAgent.ifBlank { userAgent },
                    drmScheme = url3DrmScheme.ifBlank { drmScheme },
                    drmLicense = url3DrmLicense.ifBlank { drmLicense },
                )
            )
        }
        return list
    }
}

data class StreamSource(
    val index: Int,
    val label: String,
    val rawUrl: String,
    val referer: String,
    val userAgent: String,
    val drmScheme: String,
    val drmLicense: String,
)

@Entity(tableName = "channel_groups")
data class ChannelGroupEntity(
    @PrimaryKey val name: String,
    val orderIndex: Int = 0,
    val type: String = "live",
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "favorite_categories")
data class FavoriteCategoryEntity(
    @PrimaryKey val categoryName: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val channelId: String,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val watchCount: Int = 1,
)

enum class PlayerEngineMode(val id: String) {
    STANDARD("standard"),
    LOW_LATENCY("low_latency"),
    HIGH_BUFFER("high_buffer");

    companion object {
        fun fromId(id: String?): PlayerEngineMode =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}
