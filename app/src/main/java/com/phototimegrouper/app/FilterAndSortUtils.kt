package com.phototimegrouper.app

/**
 * 大小筛选枚举
 */
enum class SizeFilter(val minSize: Long, val maxSize: Long, val label: String) {
    TINY(0L, 100 * 1024, "小于100KB"), // < 100KB
    SMALL(100 * 1024, 1024 * 1024, "100KB - 1MB"), // 100KB - 1MB
    MEDIUM(1024 * 1024, 10 * 1024 * 1024, "1MB - 10MB"), // 1MB - 10MB
    LARGE(10 * 1024 * 1024, 100 * 1024 * 1024, "10MB - 100MB"), // 10MB - 100MB
    HUGE(100 * 1024 * 1024, Long.MAX_VALUE, "大于100MB"); // > 100MB
    
    fun matches(size: Long): Boolean {
        return size >= minSize && size < maxSize
    }
}

/**
 * 排序方式枚举
 */
enum class SortOrder(val label: String) {
    DATE_DESC("日期（新到旧）"),
    DATE_ASC("日期（旧到新）"),
    NAME_ASC("名称（A-Z）"),
    NAME_DESC("名称（Z-A）"),
    SIZE_DESC("大小（大到小）"),
    SIZE_ASC("大小（小到大）")
}

/**
 * 筛选和排序工具类
 */
object FilterAndSortUtils {
    /**
     * 应用大小筛选
     */
    fun filterBySize(photos: List<PhotoItem>, sizeFilter: SizeFilter?): List<PhotoItem> {
        if (sizeFilter == null) return photos
        return photos.filter { sizeFilter.matches(it.size) }
    }
    
    /**
     * 应用排序
     */
    fun sortPhotos(photos: List<PhotoItem>, sortOrder: SortOrder): List<PhotoItem> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> photos.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> photos.sortedBy { it.dateAdded }
            SortOrder.NAME_ASC -> photos.sortedBy { it.displayName.lowercase() }
            SortOrder.NAME_DESC -> photos.sortedByDescending { it.displayName.lowercase() }
            SortOrder.SIZE_DESC -> photos.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> photos.sortedBy { it.size }
        }
    }
}
