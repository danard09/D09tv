package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import com.example.data.model.FavoriteCategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.WatchHistoryEntity

@Database(
    entities = [
        ChannelEntity::class,
        ChannelGroupEntity::class,
        FavoriteEntity::class,
        FavoriteCategoryEntity::class,
        WatchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun iptvDao(): IptvDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "d09_tv_database.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
