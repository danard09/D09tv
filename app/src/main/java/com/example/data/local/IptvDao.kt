package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import com.example.data.model.FavoriteCategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {
    @Query("SELECT * FROM channels ORDER BY orderIndex ASC, name ASC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE enabled = 1 ORDER BY orderIndex ASC, name ASC")
    fun observeEnabledChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setChannelEnabled(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE channels SET orderIndex = :orderIndex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateChannelOrder(id: String, orderIndex: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE channels SET groupName = :newGroup, updatedAt = :updatedAt WHERE groupName = :oldGroup")
    suspend fun reassignChannelsGroup(oldGroup: String, newGroup: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: String)

    @Query("DELETE FROM channels WHERE isCustom = 0")
    suspend fun deleteBundledChannels()

    @Query("SELECT * FROM channel_groups ORDER BY orderIndex ASC, name ASC")
    fun observeGroups(): Flow<List<ChannelGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<ChannelGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: ChannelGroupEntity)

    @Query("DELETE FROM channel_groups WHERE name = :name")
    suspend fun deleteGroupByName(name: String)

    @Query("UPDATE channel_groups SET orderIndex = :orderIndex WHERE name = :name")
    suspend fun updateGroupOrder(name: String, orderIndex: Int)

    @Transaction
    suspend fun renameGroupTransaction(oldName: String, newName: String, orderIndex: Int, type: String) {
        deleteGroupByName(oldName)
        upsertGroup(ChannelGroupEntity(name = newName, orderIndex = orderIndex, type = type))
        reassignChannelsGroup(oldName, newName)
    }

    @Transaction
    suspend fun deleteGroupAndReassignTransaction(groupName: String, fallbackGroup: String) {
        deleteGroupByName(groupName)
        if (fallbackGroup.isNotBlank() && fallbackGroup != groupName) {
            upsertGroup(ChannelGroupEntity(name = fallbackGroup, orderIndex = 999, type = "live"))
            reassignChannelsGroup(groupName, fallbackGroup)
        }
    }

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun removeFavorite(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean

    @Query("SELECT * FROM favorite_categories ORDER BY addedAt DESC")
    fun observeFavoriteCategories(): Flow<List<FavoriteCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteCategory(category: FavoriteCategoryEntity)

    @Query("DELETE FROM favorite_categories WHERE categoryName = :categoryName")
    suspend fun removeFavoriteCategory(categoryName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_categories WHERE categoryName = :categoryName)")
    suspend fun isFavoriteCategory(categoryName: String): Boolean

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE channelId = :channelId LIMIT 1")
    suspend fun getHistoryForChannel(channelId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()
}
