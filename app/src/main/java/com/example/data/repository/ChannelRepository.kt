package com.example.data.repository

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.data.local.IptvDao
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import com.example.data.model.FavoriteCategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.WatchHistoryEntity
import com.example.data.parser.PlaylistJsonParser
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ChannelRepository(
    private val context: Context,
    private val dao: IptvDao,
    private val prefs: UserPreferencesRepository,
) {
    companion object {
        const val BUNDLED_PLAYLIST_ASSET = "aistudio/kobani_4k.json_4d9e328f.txt"
    }

    val allChannels: Flow<List<ChannelEntity>> = dao.observeAllChannels()
    val enabledChannels: Flow<List<ChannelEntity>> = dao.observeEnabledChannels()
    val groups: Flow<List<ChannelGroupEntity>> = dao.observeGroups()
    val favorites: Flow<List<FavoriteEntity>> = dao.observeFavorites()
    val favoriteCategories: Flow<List<FavoriteCategoryEntity>> = dao.observeFavoriteCategories()
    val watchHistory: Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory()

    suspend fun ensureBundledPlaylistSeeded() = withContext(Dispatchers.IO) {
        val count = dao.getChannelCount()
        val seeded = prefs.settingsFlow.first().initialPlaylistSeeded
        if (count == 0 || !seeded) {
            seedFromBundledAsset()
            prefs.markInitialPlaylistSeeded()
        }
    }

    suspend fun restoreBundledPlaylist(): Int = withContext(Dispatchers.IO) {
        dao.deleteBundledChannels()
        seedFromBundledAsset()
    }

    private suspend fun seedFromBundledAsset(): Int {
        val rawJson = context.assets.open(BUNDLED_PLAYLIST_ASSET).bufferedReader().use { it.readText() }
        val parsed = PlaylistJsonParser.parse(rawJson, isCustomImport = false)
        if (parsed.groups.isNotEmpty()) {
            dao.upsertGroups(parsed.groups)
        }
        if (parsed.channels.isNotEmpty()) {
            dao.upsertChannels(parsed.channels)
        }
        return parsed.channels.size
    }

    suspend fun importPlaylistJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = PlaylistJsonParser.parse(jsonText, isCustomImport = false)
            if (parsed.channels.isEmpty()) {
                throw IllegalArgumentException("No valid channels in JSON")
            }
            if (parsed.groups.isNotEmpty()) {
                dao.upsertGroups(parsed.groups)
            }
            dao.upsertChannels(parsed.channels)
            parsed.channels.size
        }
    }

    suspend fun exportPlaylistJson(): String = withContext(Dispatchers.IO) {
        val currentChannels = dao.observeAllChannels().first()
        val currentGroups = dao.observeGroups().first()
        PlaylistJsonParser.exportToJson(currentChannels, currentGroups)
    }

    suspend fun saveChannel(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        if (channel.groupName.isNotBlank()) {
            val existingGroups = dao.observeGroups().first()
            if (existingGroups.none { it.name == channel.groupName }) {
                dao.upsertGroup(
                    ChannelGroupEntity(
                        name = channel.groupName,
                        orderIndex = (existingGroups.maxOfOrNull { it.orderIndex } ?: 0) + 1,
                        type = channel.type,
                    )
                )
            }
        }
        dao.upsertChannel(channel.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setChannelEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.setChannelEnabled(id, enabled)
    }

    suspend fun updateChannelOrder(id: String, orderIndex: Int) = withContext(Dispatchers.IO) {
        dao.updateChannelOrder(id, orderIndex)
    }

    suspend fun swapChannelOrder(first: ChannelEntity, second: ChannelEntity) = withContext(Dispatchers.IO) {
        val firstOrder = first.orderIndex
        val secondOrder = second.orderIndex
        val adjustedSecond = if (firstOrder == secondOrder) firstOrder + 1 else secondOrder
        dao.updateChannelOrder(first.id, adjustedSecond)
        dao.updateChannelOrder(second.id, firstOrder)
    }

    suspend fun deleteChannel(id: String) = withContext(Dispatchers.IO) {
        dao.deleteChannelById(id)
        dao.removeFavorite(id)
    }

    suspend fun addGroup(name: String) = withContext(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isEmpty()) return@withContext
        val existingGroups = dao.observeGroups().first()
        if (existingGroups.none { it.name.equals(clean, ignoreCase = true) }) {
            dao.upsertGroup(
                ChannelGroupEntity(
                    name = clean,
                    orderIndex = (existingGroups.maxOfOrNull { it.orderIndex } ?: 0) + 1,
                    type = "live",
                )
            )
        }
    }

    suspend fun renameGroup(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val cleanNew = newName.trim()
        if (cleanNew.isEmpty() || cleanNew == oldName) return@withContext
        val existing = dao.observeGroups().first().firstOrNull { it.name == oldName }
        dao.renameGroupTransaction(
            oldName = oldName,
            newName = cleanNew,
            orderIndex = existing?.orderIndex ?: 0,
            type = existing?.type ?: "live",
        )
    }

    suspend fun deleteGroup(groupName: String) = withContext(Dispatchers.IO) {
        val other = dao.observeGroups().first()
            .firstOrNull { it.name != groupName }
            ?.name
            ?: "KURD TV"
        dao.deleteGroupAndReassignTransaction(groupName, other)
        dao.removeFavoriteCategory(groupName)
    }

    suspend fun swapGroupOrder(first: ChannelGroupEntity, second: ChannelGroupEntity) = withContext(Dispatchers.IO) {
        val firstOrder = first.orderIndex
        val secondOrder = second.orderIndex
        val adjustedSecond = if (firstOrder == secondOrder) firstOrder + 1 else secondOrder
        dao.updateGroupOrder(first.name, adjustedSecond)
        dao.updateGroupOrder(second.name, firstOrder)
    }

    suspend fun toggleFavorite(channelId: String) = withContext(Dispatchers.IO) {
        if (dao.isFavorite(channelId)) {
            dao.removeFavorite(channelId)
        } else {
            dao.addFavorite(FavoriteEntity(channelId = channelId))
        }
    }

    suspend fun toggleFavoriteCategory(categoryName: String) = withContext(Dispatchers.IO) {
        if (dao.isFavoriteCategory(categoryName)) {
            dao.removeFavoriteCategory(categoryName)
        } else {
            dao.addFavoriteCategory(FavoriteCategoryEntity(categoryName = categoryName))
        }
    }

    suspend fun recordWatchHistory(channelId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getHistoryForChannel(channelId)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                channelId = channelId,
                lastWatchedAt = System.currentTimeMillis(),
                watchCount = (existing?.watchCount ?: 0) + 1,
            )
        )
        prefs.setLastWatchedChannelId(channelId)
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAppCache() = withContext(Dispatchers.IO) {
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name != "d09_tv_database.db") {
                file.deleteRecursively()
            }
        }
    }
}
