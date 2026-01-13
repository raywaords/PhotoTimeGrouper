package com.phototimegrouper.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期格式化工具类，提供线程安全的日期格式化方�?
 */
object DateFormatter {
    
    // 使用 ThreadLocal 确保线程安全
    private val dateGroupFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    private val dateTimeFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    
    private val dateHeaderFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    }
    
    private val dateHeaderInputFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    /**
     * 将时间戳（秒）格式化为日期字符串（用于分组）
     */
    fun formatDateForGroup(timestampSeconds: Long): String {
        // ThreadLocal.withInitial 保证返回值不�?null
        return requireNotNull(dateGroupFormat.get()).format(Date(timestampSeconds * 1000))
    }
    
    /**
     * 将时间戳（秒）格式化为日期时间字符串
     */
    fun formatDateTime(timestampSeconds: Long): String {
        // ThreadLocal.withInitial 保证返回值不�?null
        return requireNotNull(dateTimeFormat.get()).format(Date(timestampSeconds * 1000))
    }
    
    /**
     * 将日期字符串格式化为更易读的格式
     */
    fun formatDateHeader(dateString: String): String {
        return try {
            // ThreadLocal.withInitial 保证返回值不�?null
            val date = requireNotNull(dateHeaderInputFormat.get()).parse(dateString)
            date?.let { requireNotNull(dateHeaderFormat.get()).format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
