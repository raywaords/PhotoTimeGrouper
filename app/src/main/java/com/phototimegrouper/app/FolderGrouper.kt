package com.phototimegrouper.app

/**
 * 工具类：根据 PhotoItem 的 bucketDisplayName 分组为文件夹
 */
object FolderGrouper {

    fun groupByFolder(photos: List<PhotoItem>): List<FolderItem> {
        if (photos.isEmpty()) return emptyList()

        val grouped = photos.groupBy { it.bucketDisplayName.ifBlank { "未知文件夹" } }

        return grouped.entries.map { (name, list) ->
            val coverUri = list.firstOrNull()?.uri ?: ""
            FolderItem(
                name = name,
                count = list.size,
                coverUri = coverUri
            )
        }.sortedBy { it.name.lowercase() }
    }
}

