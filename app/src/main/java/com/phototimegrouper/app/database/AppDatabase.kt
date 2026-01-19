package com.phototimegrouper.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 应用数据库类
 * Room 数据库单例，管理照片元数据
 */
@Database(
    entities = [PhotoMetadataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun photoMetadataDao(): PhotoMetadataDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "phototimegrouper.db"
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 开发阶段：数据库版本变更时删除旧数据
                    // 生产环境应该使用 .addMigrations() 定义迁移策略
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 销毁数据库实例（用于测试）
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
