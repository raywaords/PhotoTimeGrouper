package com.phototimegrouper.app

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * 照片元数据加载工具类
 * 用于�?Exif 数据中读取照片的详细信息（如 ISO�?
 */
object PhotoMetadataLoader {
    
    /**
     * 从照�?URI 或文件路径读�?ISO 感光�?
     */
    fun loadIso(context: Context, photoItem: PhotoItem): Int {
        return try {
            val inputStream = if (photoItem.data.isNotEmpty()) {
                // 优先使用文件路径
                java.io.FileInputStream(photoItem.data)
            } else {
                // 使用 URI
                context.contentResolver.openInputStream(android.net.Uri.parse(photoItem.uri))
            }
            
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val isoString = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                
                // Exif 中的 ISO 可能是字符串格式 "100" 或数组格�?"100,100"
                isoString?.split(",")?.firstOrNull()?.toIntOrNull() ?: 0
            } ?: 0
        } catch (e: Exception) {
            // 如果读取失败，返�?0
            0
        }
    }
    
    /**
     * 从照�?URI 或文件路径读取更�?Exif 信息
     */
    fun loadExifInfo(context: Context, photoItem: PhotoItem): Map<String, String> {
        val exifInfo = mutableMapOf<String, String>()
        
        return try {
            val inputStream = if (photoItem.data.isNotEmpty()) {
                java.io.FileInputStream(photoItem.data)
            } else {
                context.contentResolver.openInputStream(android.net.Uri.parse(photoItem.uri))
            }
            
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                
                // ISO
                exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let {
                    exifInfo["ISO"] = it.split(",").firstOrNull() ?: it
                }
                
                // 焦距
                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    exifInfo["焦距"] = it
                }
                
                // 光圈
                exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
                    exifInfo["光圈"] = "f/$it"
                }
                
                // 曝光时间
                exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    val exposure = it.toDoubleOrNull()
                    if (exposure != null && exposure > 0) {
                        exifInfo["曝光时间"] = if (exposure >= 1) {
                            "${exposure.toInt()}�?
                        } else {
                            "1/${(1.0 / exposure).toInt()}�?
                        }
                    } else {
                        exifInfo["曝光时间"] = it
                    }
                }
                
                // 相机型号
                exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
                    exifInfo["相机品牌"] = it
                }
                
                exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                    exifInfo["相机型号"] = it
                }
            }
            
            exifInfo
        } catch (e: Exception) {
            exifInfo
        }
    }
}
