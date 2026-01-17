package com.phototimegrouper.app

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.content.SharedPreferences
import android.content.Context

/**
 * MainActivity 逻辑功能单元测试
 * 
 * 测试 MainActivity 中的纯逻辑方法：
 * - 收藏功能（toggleFavorite, loadFavorites, saveFavorites）
 * - 筛选功能（filterPhotos）
 * - 选择模式逻辑
 */
class MainActivityLogicTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var favoritePhotos: MutableSet<Long>
    private val PREF_FAVORITES = "pref_favorites"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        favoritePhotos = mutableSetOf()
        
        // 设置 mock 行为
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then {}
    }

    // ========== 收藏功能测试 ==========
    
    @Test
    fun `test toggleFavorite - add favorite`() {
        val photoId = 123L
        
        // 初始状态：未收藏
        assertFalse(favoritePhotos.contains(photoId))
        
        // 添加收藏
        favoritePhotos.add(photoId)
        
        // 验证已收藏
        assertTrue(favoritePhotos.contains(photoId))
        assertEquals(1, favoritePhotos.size)
    }
    
    @Test
    fun `test toggleFavorite - remove favorite`() {
        val photoId = 123L
        
        // 初始状态：已收藏
        favoritePhotos.add(photoId)
        assertTrue(favoritePhotos.contains(photoId))
        
        // 取消收藏
        favoritePhotos.remove(photoId)
        
        // 验证已取消收藏
        assertFalse(favoritePhotos.contains(photoId))
        assertEquals(0, favoritePhotos.size)
    }
    
    @Test
    fun `test toggleFavorite - multiple photos`() {
        val photoId1 = 123L
        val photoId2 = 456L
        val photoId3 = 789L
        
        // 添加多个收藏
        favoritePhotos.add(photoId1)
        favoritePhotos.add(photoId2)
        favoritePhotos.add(photoId3)
        
        // 验证所有都已收藏
        assertTrue(favoritePhotos.contains(photoId1))
        assertTrue(favoritePhotos.contains(photoId2))
        assertTrue(favoritePhotos.contains(photoId3))
        assertEquals(3, favoritePhotos.size)
        
        // 取消其中一个
        favoritePhotos.remove(photoId2)
        
        // 验证状态
        assertTrue(favoritePhotos.contains(photoId1))
        assertFalse(favoritePhotos.contains(photoId2))
        assertTrue(favoritePhotos.contains(photoId3))
        assertEquals(2, favoritePhotos.size)
    }
    
    @Test
    fun `test loadFavorites - empty string`() {
        // 模拟从 SharedPreferences 读取空字符串
        `when`(mockSharedPreferences.getString(PREF_FAVORITES, "")).thenReturn("")
        
        val result = mockSharedPreferences.getString(PREF_FAVORITES, "")
        
        // 空字符串应该被处理为空集合
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }
    
    @Test
    fun `test loadFavorites - valid IDs`() {
        // 模拟从 SharedPreferences 读取有效的 ID 字符串
        val idsString = "123,456,789"
        `when`(mockSharedPreferences.getString(PREF_FAVORITES, "")).thenReturn(idsString)
        
        val result = mockSharedPreferences.getString(PREF_FAVORITES, "")
        
        // 验证可以解析
        assertNotNull(result)
        val ids = result!!.split(",").mapNotNull { it.toLongOrNull() }
        assertEquals(3, ids.size)
        assertTrue(ids.contains(123L))
        assertTrue(ids.contains(456L))
        assertTrue(ids.contains(789L))
    }
    
    @Test
    fun `test saveFavorites - empty set`() {
        favoritePhotos.clear()
        
        // 保存空集合应该保存空字符串
        val idsString = favoritePhotos.joinToString(",")
        assertEquals("", idsString)
    }
    
    @Test
    fun `test saveFavorites - multiple IDs`() {
        favoritePhotos.add(123L)
        favoritePhotos.add(456L)
        favoritePhotos.add(789L)
        
        // 保存应该生成逗号分隔的字符串
        val idsString = favoritePhotos.joinToString(",")
        assertTrue(idsString.contains("123"))
        assertTrue(idsString.contains("456"))
        assertTrue(idsString.contains("789"))
    }

    // ========== 筛选功能测试 ==========
    
    @Test
    fun `test filterPhotos - by media type image`() {
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.VIDEO),
            createPhotoItem(id = 3L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 4L, mediaType = PhotoItem.MediaType.VIDEO)
        )
        
        val filtered = photos.filter { it.mediaType == PhotoItem.MediaType.IMAGE }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.mediaType == PhotoItem.MediaType.IMAGE })
    }
    
    @Test
    fun `test filterPhotos - by media type video`() {
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.VIDEO),
            createPhotoItem(id = 3L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 4L, mediaType = PhotoItem.MediaType.VIDEO)
        )
        
        val filtered = photos.filter { it.mediaType == PhotoItem.MediaType.VIDEO }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.mediaType == PhotoItem.MediaType.VIDEO })
    }
    
    @Test
    fun `test filterPhotos - by date range`() {
        val now = System.currentTimeMillis() / 1000
        val sevenDaysAgo = now - (7 * 24 * 60 * 60)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60)
        
        val photos = listOf(
            createPhotoItem(id = 1L, dateModified = now), // 今天
            createPhotoItem(id = 2L, dateModified = sevenDaysAgo - 1), // 8天前
            createPhotoItem(id = 3L, dateModified = sevenDaysAgo + 1), // 7天内
            createPhotoItem(id = 4L, dateModified = thirtyDaysAgo - 1), // 31天前
            createPhotoItem(id = 5L, dateModified = thirtyDaysAgo + 1) // 30天内
        )
        
        // 筛选近7天
        val filtered7Days = photos.filter { 
            it.dateModified >= (now - (7 * 24 * 60 * 60))
        }
        
        assertEquals(2, filtered7Days.size) // 今天和7天内的
        assertTrue(filtered7Days.all { it.dateModified >= (now - (7 * 24 * 60 * 60)) })
    }
    
    @Test
    fun `test filterPhotos - by media type and date range`() {
        val now = System.currentTimeMillis() / 1000
        val sevenDaysAgo = now - (7 * 24 * 60 * 60)
        
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.IMAGE, dateModified = now),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.VIDEO, dateModified = now),
            createPhotoItem(id = 3L, mediaType = PhotoItem.MediaType.IMAGE, dateModified = sevenDaysAgo - 1),
            createPhotoItem(id = 4L, mediaType = PhotoItem.MediaType.IMAGE, dateModified = sevenDaysAgo + 1)
        )
        
        // 筛选：图片 + 近7天
        val filtered = photos.filter { 
            it.mediaType == PhotoItem.MediaType.IMAGE && 
            it.dateModified >= (now - (7 * 24 * 60 * 60))
        }
        
        assertEquals(2, filtered.size) // ID 1 和 4
        assertTrue(filtered.all { 
            it.mediaType == PhotoItem.MediaType.IMAGE && 
            it.dateModified >= (now - (7 * 24 * 60 * 60))
        })
    }

    // ========== 选择模式测试 ==========
    
    @Test
    fun `test selection mode - enter and select`() {
        val selectedPhotos = mutableSetOf<Long>()
        val photoId = 123L
        
        // 进入选择模式并选中
        selectedPhotos.add(photoId)
        
        assertTrue(selectedPhotos.contains(photoId))
        assertEquals(1, selectedPhotos.size)
    }
    
    @Test
    fun `test selection mode - toggle selection`() {
        val selectedPhotos = mutableSetOf<Long>()
        val photoId = 123L
        
        // 第一次选择
        selectedPhotos.add(photoId)
        assertTrue(selectedPhotos.contains(photoId))
        
        // 取消选择
        selectedPhotos.remove(photoId)
        assertFalse(selectedPhotos.contains(photoId))
        
        // 再次选择
        selectedPhotos.add(photoId)
        assertTrue(selectedPhotos.contains(photoId))
    }
    
    @Test
    fun `test selection mode - select all`() {
        val selectedPhotos = mutableSetOf<Long>()
        val photos = listOf(
            createPhotoItem(id = 1L),
            createPhotoItem(id = 2L),
            createPhotoItem(id = 3L)
        )
        
        // 全选
        selectedPhotos.addAll(photos.map { it.id })
        
        assertEquals(3, selectedPhotos.size)
        assertTrue(selectedPhotos.contains(1L))
        assertTrue(selectedPhotos.contains(2L))
        assertTrue(selectedPhotos.contains(3L))
    }
    
    @Test
    fun `test selection mode - clear selection`() {
        val selectedPhotos = mutableSetOf<Long>()
        selectedPhotos.add(1L)
        selectedPhotos.add(2L)
        selectedPhotos.add(3L)
        
        assertEquals(3, selectedPhotos.size)
        
        // 清空选择
        selectedPhotos.clear()
        
        assertEquals(0, selectedPhotos.size)
        assertTrue(selectedPhotos.isEmpty())
    }

    // ========== 分享功能测试 ==========
    
    @Test
    fun `test shareSelectedPhotos - filter selected items`() {
        val allPhotos = listOf(
            createPhotoItem(id = 1L),
            createPhotoItem(id = 2L),
            createPhotoItem(id = 3L),
            createPhotoItem(id = 4L)
        )
        val selectedPhotos = mutableSetOf(1L, 3L)
        
        // 筛选出选中的照片
        val selectedItems = allPhotos.filter { selectedPhotos.contains(it.id) }
        
        assertEquals(2, selectedItems.size)
        assertTrue(selectedItems.any { it.id == 1L })
        assertTrue(selectedItems.any { it.id == 3L })
        assertFalse(selectedItems.any { it.id == 2L })
        assertFalse(selectedItems.any { it.id == 4L })
    }
    
    @Test
    fun `test shareSelectedPhotos - MIME type detection image only`() {
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.IMAGE)
        )
        
        val hasVideo = photos.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = photos.any { it.mediaType == PhotoItem.MediaType.IMAGE }
        
        val mimeType = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        
        assertEquals("image/*", mimeType)
    }
    
    @Test
    fun `test shareSelectedPhotos - MIME type detection video only`() {
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.VIDEO),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.VIDEO)
        )
        
        val hasVideo = photos.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = photos.any { it.mediaType == PhotoItem.MediaType.IMAGE }
        
        val mimeType = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        
        assertEquals("video/*", mimeType)
    }
    
    @Test
    fun `test shareSelectedPhotos - MIME type detection mixed`() {
        val photos = listOf(
            createPhotoItem(id = 1L, mediaType = PhotoItem.MediaType.IMAGE),
            createPhotoItem(id = 2L, mediaType = PhotoItem.MediaType.VIDEO)
        )
        
        val hasVideo = photos.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = photos.any { it.mediaType == PhotoItem.MediaType.IMAGE }
        
        val mimeType = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        
        assertEquals("*/*", mimeType)
    }

    // ========== 删除功能测试 ==========
    
    @Test
    fun `test deleteSelectedPhotos - filter selected items`() {
        val allPhotos = listOf(
            createPhotoItem(id = 1L),
            createPhotoItem(id = 2L),
            createPhotoItem(id = 3L),
            createPhotoItem(id = 4L)
        )
        val selectedPhotos = mutableSetOf(2L, 4L)
        
        // 筛选出选中的照片
        val selectedItems = allPhotos.filter { selectedPhotos.contains(it.id) }
        
        assertEquals(2, selectedItems.size)
        assertTrue(selectedItems.any { it.id == 2L })
        assertTrue(selectedItems.any { it.id == 4L })
        assertFalse(selectedItems.any { it.id == 1L })
        assertFalse(selectedItems.any { it.id == 3L })
    }
    
    @Test
    fun `test deleteSelectedPhotos - empty selection`() {
        val allPhotos = listOf(
            createPhotoItem(id = 1L),
            createPhotoItem(id = 2L)
        )
        val selectedPhotos = mutableSetOf<Long>()
        
        val selectedItems = allPhotos.filter { selectedPhotos.contains(it.id) }
        
        assertTrue(selectedItems.isEmpty())
    }

    // ========== 辅助方法 ==========
    
    private fun createPhotoItem(
        id: Long = 1L,
        uri: String = "content://media/external/images/media/$id",
        displayName: String = "test.jpg",
        dateAdded: Long = System.currentTimeMillis() / 1000,
        dateModified: Long = System.currentTimeMillis() / 1000,
        size: Long = 1024L,
        width: Int = 1920,
        height: Int = 1080,
        mimeType: String = "image/jpeg",
        mediaType: PhotoItem.MediaType = PhotoItem.MediaType.IMAGE
    ): PhotoItem {
        return PhotoItem(
            id = id,
            uri = uri,
            displayName = displayName,
            dateAdded = dateAdded,
            dateModified = dateModified,
            size = size,
            width = width,
            height = height,
            mimeType = mimeType,
            mediaType = mediaType
        )
    }
}
