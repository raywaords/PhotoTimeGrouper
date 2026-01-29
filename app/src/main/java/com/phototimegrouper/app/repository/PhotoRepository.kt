package com.phototimegrouper.app.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.phototimegrouper.app.PhotoItem
import com.phototimegrouper.app.database.AppDatabase
import com.phototimegrouper.app.database.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 照片数据仓库
 * 整合 MediaStore 数据源和 Room 数据库，提供统一的数据访问接口
 */
class PhotoRepository private constructor(context: Context) {
    
    private val appContext: Context = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val photoDao = database.photoMetadataDao()
    private val contentResolver: ContentResolver = appContext.contentResolver
    
    companion object {
        @Volatile
        private var INSTANCE: PhotoRepository? = null
        
        fun getInstance(context: Context): PhotoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PhotoRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // ==================== 照片加载（从 MediaStore）====================
    
    /**
     * 从 MediaStore 加载所有照片和视频
     * 返回 PhotoItem 列表（用于显示）
     */
    suspend fun loadPhotosFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<PhotoItem>()
        
        // 加载图片
        allMedia.addAll(loadImagesFromMediaStore())
        
        // 加载视频
        allMedia.addAll(loadVideosFromMediaStore())
        
        // 按日期排序（最新的在前）
        return@withContext allMedia.sortedByDescending { it.dateAdded }
    }
    
    /**
     * 从 MediaStore 加载图片
     * Android 10+ 使用最小 projection，避免请求已废弃列导致部分设备返回不全（如 Camera 目录）
     */
    private suspend fun loadImagesFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // 统一使用完整 projection，通过 getColumnIndex 动态判断可用列，既保证兼容性，又能拿到尺寸/格式等信息
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            // 可选列：部分设备/Android 版本可能缺失，使用 getColumnIndex 避免漏掉任何照片
            val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
                val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
                val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucket = if (bucketColumn >= 0) cursor.getString(bucketColumn) ?: "" else ""
                val data = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                
                val imageUri = Uri.withAppendedPath(uri, id.toString())
                
                photos.add(
                    PhotoItem(
                        id = id,
                        uri = imageUri.toString(),
                        displayName = name,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        bucketDisplayName = bucket,
                        data = data,
                        mediaType = PhotoItem.MediaType.IMAGE
                    )
                )
            }
        }
        
        photos
    }
    
    /**
     * 从 MediaStore 加载视频
     * Android 10+ 使用最小 projection，避免请求已废弃列导致部分设备返回不全
     */
    private suspend fun loadVideosFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<PhotoItem>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        // 同样统一使用完整 projection，结合 getColumnIndex 处理可选列
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        
        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
                val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
                val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucket = if (bucketColumn >= 0) cursor.getString(bucketColumn) ?: "" else ""
                val data = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                
                val videoUri = Uri.withAppendedPath(uri, id.toString())
                
                videos.add(
                    PhotoItem(
                        id = id,
                        uri = videoUri.toString(),
                        displayName = name,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        bucketDisplayName = bucket,
                        data = data,
                        mediaType = PhotoItem.MediaType.VIDEO
                    )
                )
            }
        }
        
        videos
    }
    
    // ==================== 数据同步（MediaStore ↔ Database）====================
    
    /**
     * 同步 MediaStore 数据到数据库
     * 加载照片后调用此方法，将数据写入数据库
     */
    suspend fun syncMediaStoreToDatabase(photos: List<PhotoItem>) = withContext(Dispatchers.IO) {
        // 将 PhotoItem 转换为 PhotoMetadataEntity
        val entities = photos.map { photo ->
            // 先从数据库读取现有数据（如果存在）
            val existing = photoDao.getPhotoById(photo.id)
            
            PhotoMetadataEntity(
                id = photo.id,
                uri = photo.uri,
                displayName = photo.displayName,
                dateAdded = photo.dateAdded,
                dateModified = photo.dateModified,
                mediaType = photo.mediaType.name,
                size = photo.size,
                width = photo.width,
                height = photo.height,
                mimeType = photo.mimeType,
                bucketDisplayName = photo.bucketDisplayName,
                data = photo.data,
                iso = photo.iso,
                // 保留数据库中的状态（如果存在）
                isFavorite = existing?.isFavorite ?: false,
                isDeleted = existing?.isDeleted ?: false,
                deletedAt = existing?.deletedAt,
                isHidden = existing?.isHidden ?: false,
                createdAt = existing?.createdAt ?: (System.currentTimeMillis() / 1000),
                updatedAt = System.currentTimeMillis() / 1000
            )
        }
        
        // 批量插入或更新
        photoDao.insertPhotos(entities)
        
        // 清理数据库中不存在于 MediaStore 的照片（删除已从设备删除的照片记录）
        val existingIds = photos.map { it.id }
        photoDao.deleteOrphanedPhotos(existingIds)
    }
    
    /**
     * 应用数据库状态到 PhotoItem 列表
     * 将收藏、删除等状态应用到照片列表
     */
    suspend fun applyDatabaseStateToPhotos(photos: List<PhotoItem>): List<PhotoItem> = withContext(Dispatchers.IO) {
        // 从数据库读取所有状态
        val dbIds = photoDao.getAllPhotoIds()
        val dbEntities = mutableMapOf<Long, PhotoMetadataEntity>()
        
        // 批量读取状态（可以优化）
        dbIds.forEach { id ->
            photoDao.getPhotoById(id)?.let { entity ->
                dbEntities[id] = entity
            }
        }
        
        // 注意：PhotoItem 是不可变的，如果需要存储状态信息，可能需要扩展 PhotoItem
        // 或者通过单独的 Map 存储状态
        // 当前实现中，状态通过独立的集合管理（如 favoritePhotos）
        photos
    }
    
    // ==================== 收藏操作 ====================
    
    /**
     * 获取所有收藏的照片 ID
     */
    suspend fun getFavoritePhotoIds(): Set<Long> = withContext(Dispatchers.IO) {
        // 使用 Flow.first() 获取第一个值（Flow 会发送一次当前数据）
        photoDao.getFavoritePhotos().first().map { it.id }.toSet()
    }
    
    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(photoId: Long): Boolean = withContext(Dispatchers.IO) {
        val current = photoDao.isFavorite(photoId) ?: false
        val newState = !current
        val updatedAt = System.currentTimeMillis() / 1000
        photoDao.setFavorite(photoId, newState, updatedAt)
        newState
    }
    
    /**
     * 检查是否收藏
     */
    suspend fun isFavorite(photoId: Long): Boolean = withContext(Dispatchers.IO) {
        photoDao.isFavorite(photoId) ?: false
    }
    
    /**
     * 批量设置收藏状态
     */
    suspend fun setFavoriteBatch(photoIds: List<Long>, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val updatedAt = System.currentTimeMillis() / 1000
        photoDao.setFavoriteBatch(photoIds, isFavorite, updatedAt)
    }
    
    // ==================== 搜索操作 ====================
    
    /**
     * 搜索照片（按文件名）
     */
    suspend fun searchPhotos(query: String): List<PhotoMetadataEntity> = withContext(Dispatchers.IO) {
        photoDao.searchPhotos(query)
    }
    
    /**
     * 搜索照片（Flow）
     */
    fun searchPhotosFlow(query: String): Flow<List<PhotoMetadataEntity>> {
        return photoDao.searchPhotosFlow(query)
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 软删除照片
     */
    suspend fun softDeletePhoto(photoId: Long) = withContext(Dispatchers.IO) {
        val deletedAt = System.currentTimeMillis() / 1000
        val updatedAt = deletedAt
        photoDao.softDeletePhoto(photoId, deletedAt, updatedAt)
    }
    
    /**
     * 彻底删除照片
     */
    suspend fun hardDeletePhoto(photoId: Long) = withContext(Dispatchers.IO) {
        photoDao.hardDeletePhoto(photoId)
    }
    
    /**
     * 恢复删除
     */
    suspend fun restorePhoto(photoId: Long) = withContext(Dispatchers.IO) {
        val updatedAt = System.currentTimeMillis() / 1000
        photoDao.restorePhoto(photoId, updatedAt)
    }
    
    /**
     * 批量软删除
     */
    suspend fun softDeletePhotos(photoIds: List<Long>) = withContext(Dispatchers.IO) {
        val deletedAt = System.currentTimeMillis() / 1000
        val updatedAt = deletedAt
        photoDao.softDeletePhotos(photoIds, deletedAt, updatedAt)
    }

    /**
     * 获取所有已删除照片（回收站）Flow
     */
    fun getDeletedPhotosFlow(): Flow<List<PhotoMetadataEntity>> {
        return photoDao.getDeletedPhotos()
    }

    /**
     * 获取已删除照片的 ID 集合（用于从主列表中过滤）
     */
    suspend fun getDeletedPhotoIds(): Set<Long> = withContext(Dispatchers.IO) {
        photoDao.getDeletedPhotos().first().map { it.id }.toSet()
    }

    /**
     * 获取已在回收站中超过指定天数的照片（用于自动清理）
     * @param days 天数，例如 30 表示超过 30 天
     */
    suspend fun getExpiredDeletedPhotos(days: Long): List<PhotoMetadataEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        val threshold = now - days * 24 * 60 * 60
        photoDao.getExpiredDeletedPhotos(threshold)
    }
}
