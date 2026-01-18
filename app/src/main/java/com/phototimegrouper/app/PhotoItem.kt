package com.phototimegrouper.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val dateModified: Long,
    // 扩展字段
    val size: Long = 0L,                    // 文件大小（字节）
    val width: Int = 0,                     // 宽度（像素）
    val height: Int = 0,                    // 高度（像素）
    val mimeType: String = "",              // MIME类型（如 image/jpeg, video/mp4�?
    val bucketDisplayName: String = "",     // 存储文件夹名称（来源�?
    val data: String = "",                  // 文件路径
    val iso: Int = 0,                       // ISO感光�?
    val mediaType: MediaType = MediaType.IMAGE  // 媒体类型：图片或视频
) : Parcelable {
    
    /**
     * 媒体类型枚举
     */
    enum class MediaType {
        IMAGE,  // 图片
        VIDEO   // 视频
    }
    
    /**
     * 获取格式化的文件大小
     */
    fun getFormattedSize(): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }
    
    /**
     * 获取分辨率字符串
     */
    fun getResolution(): String {
        return if (width > 0 && height > 0) {
            "${width} × ${height}"
        } else {
            "未知"
        }
    }
    
    /**
     * 获取照片格式（从MIME类型提取�?
     */
    fun getFormat(): String {
        val raw = when {
            mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "JPEG"
            mimeType.contains("png", ignoreCase = true) -> "PNG"
            mimeType.contains("gif", ignoreCase = true) -> "GIF"
            mimeType.contains("webp", ignoreCase = true) -> "WebP"
            mimeType.contains("mp4", ignoreCase = true) -> "MP4"
            mimeType.contains("mov", ignoreCase = true) || mimeType.contains("quicktime", ignoreCase = true) -> "MOV"
            mimeType.contains("avi", ignoreCase = true) || mimeType.contains("x-msvideo", ignoreCase = true) -> "AVI"
            mimeType.contains("mkv", ignoreCase = true) -> "MKV"
            mimeType.contains("3gp", ignoreCase = true) -> "3GP"
            mimeType.isNotEmpty() -> mimeType.substringAfterLast("/").uppercase()
            else -> "未知"
        }
        // 防止编码问题导致的乱码字符，强制过滤为可见 ASCII 或常见中文
        return raw.filter { ch ->
            // 基本拉丁字母数字和常见标点
            ch.code in 32..126 ||
            // 简体中文常用区间
            ch.code in 0x4E00..0x9FFF
        }
    }
    
    /**
     * 判断是否为视�?
     * 直接使用 mediaType 判断，确保准确�?
     */
    fun isVideo(): Boolean {
        // 直接使用 mediaType 判断（最准确，因为数据加载时已经明确设置�?
        return mediaType == MediaType.VIDEO
    }
    
    /**
     * 判断是否为图�?
     * 直接使用 mediaType 判断，确保准确�?
     */
    fun isImage(): Boolean {
        // 直接使用 mediaType 判断（最准确，因为数据加载时已经明确设置�?
        return mediaType == MediaType.IMAGE
    }
}
