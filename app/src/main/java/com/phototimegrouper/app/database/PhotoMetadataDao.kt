package com.phototimegrouper.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 照片元数据 DAO（Data Access Object）
 * 定义数据库操作接口
 */
@Dao
interface PhotoMetadataDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有照片（未删除、未隐藏）
     */
    @Query("SELECT * FROM photo_metadata WHERE isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    fun getAllPhotos(): Flow<List<PhotoMetadataEntity>>
    
    /**
     * 获取收藏的照片
     */
    @Query("SELECT * FROM photo_metadata WHERE isFavorite = 1 AND isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    fun getFavoritePhotos(): Flow<List<PhotoMetadataEntity>>
    
    /**
     * 获取已删除的照片（回收站）
     */
    @Query("SELECT * FROM photo_metadata WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedPhotos(): Flow<List<PhotoMetadataEntity>>
    
    /**
     * 获取隐藏的照片
     */
    @Query("SELECT * FROM photo_metadata WHERE isHidden = 1 AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getHiddenPhotos(): Flow<List<PhotoMetadataEntity>>
    
    /**
     * 根据 ID 获取照片
     */
    @Query("SELECT * FROM photo_metadata WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): PhotoMetadataEntity?
    
    /**
     * 根据 ID 获取照片（Flow）
     */
    @Query("SELECT * FROM photo_metadata WHERE id = :photoId")
    fun getPhotoByIdFlow(photoId: Long): Flow<PhotoMetadataEntity?>
    
    /**
     * 搜索照片（按文件名）
     */
    @Query("SELECT * FROM photo_metadata WHERE displayName LIKE '%' || :query || '%' AND isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    suspend fun searchPhotos(query: String): List<PhotoMetadataEntity>
    
    /**
     * 搜索照片（Flow）
     */
    @Query("SELECT * FROM photo_metadata WHERE displayName LIKE '%' || :query || '%' AND isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    fun searchPhotosFlow(query: String): Flow<List<PhotoMetadataEntity>>
    
    /**
     * 按日期范围查询照片
     */
    @Query("SELECT * FROM photo_metadata WHERE dateAdded >= :startTimestamp AND dateAdded <= :endTimestamp AND isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    suspend fun getPhotosByDateRange(startTimestamp: Long, endTimestamp: Long): List<PhotoMetadataEntity>
    
    /**
     * 按媒体类型查询照片
     */
    @Query("SELECT * FROM photo_metadata WHERE mediaType = :mediaType AND isDeleted = 0 AND isHidden = 0 ORDER BY dateAdded DESC")
    suspend fun getPhotosByMediaType(mediaType: String): List<PhotoMetadataEntity>
    
    // ==================== 插入和更新操作 ====================
    
    /**
     * 插入单个照片
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoMetadataEntity)
    
    /**
     * 批量插入照片
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoMetadataEntity>)
    
    /**
     * 更新单个照片
     */
    @Update
    suspend fun updatePhoto(photo: PhotoMetadataEntity)
    
    /**
     * 批量更新照片
     */
    @Update
    suspend fun updatePhotos(photos: List<PhotoMetadataEntity>)
    
    // ==================== 删除操作 ====================
    
    /**
     * 软删除：标记为已删除
     */
    @Query("UPDATE photo_metadata SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :photoId")
    suspend fun softDeletePhoto(photoId: Long, deletedAt: Long, updatedAt: Long)
    
    /**
     * 彻底删除：从数据库移除记录
     */
    @Query("DELETE FROM photo_metadata WHERE id = :photoId")
    suspend fun hardDeletePhoto(photoId: Long)
    
    /**
     * 恢复删除：取消删除标记
     */
    @Query("UPDATE photo_metadata SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :photoId")
    suspend fun restorePhoto(photoId: Long, updatedAt: Long)
    
    /**
     * 批量软删除
     */
    @Query("UPDATE photo_metadata SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id IN (:photoIds)")
    suspend fun softDeletePhotos(photoIds: List<Long>, deletedAt: Long, updatedAt: Long)
    
    // ==================== 收藏操作 ====================
    
    /**
     * 设置收藏状态
     */
    @Query("UPDATE photo_metadata SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :photoId")
    suspend fun setFavorite(photoId: Long, isFavorite: Boolean, updatedAt: Long)
    
    /**
     * 批量设置收藏状态
     */
    @Query("UPDATE photo_metadata SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id IN (:photoIds)")
    suspend fun setFavoriteBatch(photoIds: List<Long>, isFavorite: Boolean, updatedAt: Long)
    
    /**
     * 检查是否收藏
     */
    @Query("SELECT isFavorite FROM photo_metadata WHERE id = :photoId")
    suspend fun isFavorite(photoId: Long): Boolean?
    
    // ==================== 隐藏操作 ====================
    
    /**
     * 设置隐藏状态
     */
    @Query("UPDATE photo_metadata SET isHidden = :isHidden, updatedAt = :updatedAt WHERE id = :photoId")
    suspend fun setHidden(photoId: Long, isHidden: Boolean, updatedAt: Long)
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取照片总数（未删除、未隐藏）
     */
    @Query("SELECT COUNT(*) FROM photo_metadata WHERE isDeleted = 0 AND isHidden = 0")
    suspend fun getPhotoCount(): Int
    
    /**
     * 获取收藏照片数量
     */
    @Query("SELECT COUNT(*) FROM photo_metadata WHERE isFavorite = 1 AND isDeleted = 0 AND isHidden = 0")
    suspend fun getFavoriteCount(): Int
    
    /**
     * 获取已删除照片数量
     */
    @Query("SELECT COUNT(*) FROM photo_metadata WHERE isDeleted = 1")
    suspend fun getDeletedCount(): Int
    
    // ==================== 同步操作 ====================
    
    /**
     * 删除数据库中不存在于 MediaStore 的照片（清理孤儿记录）
     */
    @Query("DELETE FROM photo_metadata WHERE id NOT IN (:existingIds)")
    suspend fun deleteOrphanedPhotos(existingIds: List<Long>)
    
    /**
     * 获取所有照片 ID（用于同步）
     */
    @Query("SELECT id FROM photo_metadata")
    suspend fun getAllPhotoIds(): List<Long>
}
