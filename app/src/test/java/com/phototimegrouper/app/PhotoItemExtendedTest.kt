package com.phototimegrouper.app

import org.junit.Assert.*
import org.junit.Test

/**
 * PhotoItem 扩展方法单元测试
 * 
 * 测试新增的方法：
 * - getFormattedSize() - 文件大小格式化
 * - getResolution() - 分辨率格式化
 * - getFormat() - 文件格式识别
 * - isVideo() / isImage() - 媒体类型判断
 */
class PhotoItemExtendedTest {

    // ========== getFormattedSize() 测试 ==========
    
    @Test
    fun `test getFormattedSize - bytes`() {
        val photo = createPhotoItem(size = 512L)
        val result = photo.getFormattedSize()
        assertEquals("512 B", result)
    }

    @Test
    fun `test getFormattedSize - KB`() {
        val photo = createPhotoItem(size = 2048L) // 2 KB
        val result = photo.getFormattedSize()
        assertTrue(result.contains("KB"))
        assertTrue(result.contains("2.00"))
    }

    @Test
    fun `test getFormattedSize - MB`() {
        val photo = createPhotoItem(size = 2 * 1024 * 1024L) // 2 MB
        val result = photo.getFormattedSize()
        assertTrue(result.contains("MB"))
        assertTrue(result.contains("2.00"))
    }

    @Test
    fun `test getFormattedSize - GB`() {
        val photo = createPhotoItem(size = 2 * 1024 * 1024 * 1024L) // 2 GB
        val result = photo.getFormattedSize()
        assertTrue(result.contains("GB"))
        assertTrue(result.contains("2.00"))
    }

    @Test
    fun `test getFormattedSize - zero`() {
        val photo = createPhotoItem(size = 0L)
        val result = photo.getFormattedSize()
        assertEquals("0 B", result)
    }

    @Test
    fun `test getFormattedSize - boundary KB`() {
        val photo = createPhotoItem(size = 1023L) // 小于 1 KB
        val result = photo.getFormattedSize()
        assertEquals("1023 B", result)
    }

    // ========== getResolution() 测试 ==========
    
    @Test
    fun `test getResolution - valid dimensions`() {
        val photo = createPhotoItem(width = 1920, height = 1080)
        val result = photo.getResolution()
        assertTrue(result.contains("1920"))
        assertTrue(result.contains("1080"))
    }

    @Test
    fun `test getResolution - zero width`() {
        val photo = createPhotoItem(width = 0, height = 1080)
        val result = photo.getResolution()
        assertEquals("未知", result)
    }

    @Test
    fun `test getResolution - zero height`() {
        val photo = createPhotoItem(width = 1920, height = 0)
        val result = photo.getResolution()
        assertEquals("未知", result)
    }

    @Test
    fun `test getResolution - both zero`() {
        val photo = createPhotoItem(width = 0, height = 0)
        val result = photo.getResolution()
        assertEquals("未知", result)
    }

    // ========== getFormat() 测试 ==========
    
    @Test
    fun `test getFormat - JPEG`() {
        val photo1 = createPhotoItem(mimeType = "image/jpeg")
        val photo2 = createPhotoItem(mimeType = "image/JPEG")
        val photo3 = createPhotoItem(mimeType = "image/jpg")
        
        assertEquals("JPEG", photo1.getFormat())
        assertEquals("JPEG", photo2.getFormat())
        assertEquals("JPEG", photo3.getFormat())
    }

    @Test
    fun `test getFormat - PNG`() {
        val photo = createPhotoItem(mimeType = "image/png")
        assertEquals("PNG", photo.getFormat())
    }

    @Test
    fun `test getFormat - GIF`() {
        val photo = createPhotoItem(mimeType = "image/gif")
        assertEquals("GIF", photo.getFormat())
    }

    @Test
    fun `test getFormat - WebP`() {
        val photo = createPhotoItem(mimeType = "image/webp")
        assertEquals("WebP", photo.getFormat())
    }

    @Test
    fun `test getFormat - MP4`() {
        val photo = createPhotoItem(mimeType = "video/mp4")
        assertEquals("MP4", photo.getFormat())
    }

    @Test
    fun `test getFormat - MOV`() {
        val photo = createPhotoItem(mimeType = "video/quicktime")
        assertEquals("MOV", photo.getFormat())
    }

    @Test
    fun `test getFormat - AVI`() {
        val photo = createPhotoItem(mimeType = "video/x-msvideo")
        assertEquals("AVI", photo.getFormat())
    }

    @Test
    fun `test getFormat - unknown`() {
        val photo = createPhotoItem(mimeType = "")
        assertEquals("未知", photo.getFormat())
    }

    @Test
    fun `test getFormat - custom mime type`() {
        val photo = createPhotoItem(mimeType = "image/custom")
        assertEquals("CUSTOM", photo.getFormat())
    }

    @Test
    fun `test getFormat - filters invalid characters`() {
        // 测试过滤乱码字符
        val photo = createPhotoItem(mimeType = "image/jpeg")
        val format = photo.getFormat()
        // 应该只包含可见字符
        assertTrue(format.all { it.isLetterOrDigit() || it.isWhitespace() })
    }

    // ========== isVideo() / isImage() 测试 ==========
    
    @Test
    fun `test isVideo - returns true for video`() {
        val photo = createPhotoItem(mediaType = PhotoItem.MediaType.VIDEO)
        assertTrue(photo.isVideo())
        assertFalse(photo.isImage())
    }

    @Test
    fun `test isVideo - returns false for image`() {
        val photo = createPhotoItem(mediaType = PhotoItem.MediaType.IMAGE)
        assertFalse(photo.isVideo())
        assertTrue(photo.isImage())
    }

    @Test
    fun `test isImage - returns true for image`() {
        val photo = createPhotoItem(mediaType = PhotoItem.MediaType.IMAGE)
        assertTrue(photo.isImage())
    }

    @Test
    fun `test isImage - returns false for video`() {
        val photo = createPhotoItem(mediaType = PhotoItem.MediaType.VIDEO)
        assertFalse(photo.isImage())
    }

    // ========== 辅助方法 ==========
    
    private fun createPhotoItem(
        id: Long = 1L,
        uri: String = "content://test",
        displayName: String = "test.jpg",
        dateAdded: Long = 1000L,
        dateModified: Long = 1000L,
        size: Long = 0L,
        width: Int = 0,
        height: Int = 0,
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
