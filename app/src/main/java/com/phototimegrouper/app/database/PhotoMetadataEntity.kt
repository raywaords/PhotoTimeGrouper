package com.phototimegrouper.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 照片元数据实体类
 * 用于 Room 数据库存储照片的元数据和状态信息
 */
@Entity(
    tableName = "photo_metadata",
    indices = [
        Index(value = ["isFavorite"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isHidden"]),
        Index(value = ["dateAdded"]),
        Index(value = ["mediaType"])
    ]
)
data class PhotoMetadataEntity(
    @PrimaryKey
    val id: Long,                              // MediaStore ID（主键）
    
    // 基本信息
    val uri: String,                           // 照片 URI
    val displayName: String,                   // 显示名称
    val dateAdded: Long,                       // 添加时间（Unix 时间戳，秒）
    val dateModified: Long,                    // 修改时间（Unix 时间戳，秒）
    
    // 媒体类型和格式
    val mediaType: String,                     // 媒体类型（IMAGE/VIDEO）
    val size: Long = 0L,                       // 文件大小（字节）
    val width: Int = 0,                        // 宽度（像素）
    val height: Int = 0,                       // 高度（像素）
    val mimeType: String = "",                 // MIME 类型（如 image/jpeg, video/mp4）
    
    // 位置和路径
    val bucketDisplayName: String = "",        // 存储文件夹名称（来源）
    val data: String = "",                     // 文件路径
    
    // 元数据
    val iso: Int = 0,                          // ISO 感光度
    
    // 状态标志
    val isFavorite: Boolean = false,           // 是否收藏
    val isDeleted: Boolean = false,            // 是否已删除（软删除）
    val deletedAt: Long? = null,               // 删除时间戳（Unix 时间戳，秒）
    val isHidden: Boolean = false,             // 是否隐藏（私密相册）
    
    // 时间戳
    val createdAt: Long = System.currentTimeMillis() / 1000,  // 记录创建时间（秒）
    val updatedAt: Long = System.currentTimeMillis() / 1000   // 记录更新时间（秒）
)
