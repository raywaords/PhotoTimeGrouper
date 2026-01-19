package com.phototimegrouper.app.database

import org.junit.Assert.*
import org.junit.Test

/**
 * PhotoMetadataEntity 实体类单元测试
 */
class PhotoMetadataEntityTest {

    @Test
    fun `test PhotoMetadataEntity creation with default values`() {
        val entity = PhotoMetadataEntity(
            id = 1L,
            uri = "content://media/external/images/media/1",
            displayName = "test.jpg",
            dateAdded = 1609459200L,
            dateModified = 1609459200L,
            mediaType = "IMAGE"
        )

        assertEquals(1L, entity.id)
        assertEquals("content://media/external/images/media/1", entity.uri)
        assertEquals("test.jpg", entity.displayName)
        assertEquals(1609459200L, entity.dateAdded)
        assertEquals(1609459200L, entity.dateModified)
        assertEquals("IMAGE", entity.mediaType)
        
        // 检查默认值
        assertEquals(0L, entity.size)
        assertEquals(0, entity.width)
        assertEquals(0, entity.height)
        assertEquals("", entity.mimeType)
        assertEquals("", entity.bucketDisplayName)
        assertEquals("", entity.data)
        assertEquals(0, entity.iso)
        assertFalse(entity.isFavorite)
        assertFalse(entity.isDeleted)
        assertNull(entity.deletedAt)
        assertFalse(entity.isHidden)
        assertTrue(entity.createdAt > 0)
        assertTrue(entity.updatedAt > 0)
    }

    @Test
    fun `test PhotoMetadataEntity with all fields`() {
        val currentTime = System.currentTimeMillis() / 1000
        val entity = PhotoMetadataEntity(
            id = 123L,
            uri = "content://media/external/images/media/123",
            displayName = "photo.jpg",
            dateAdded = 1609459200L,
            dateModified = 1609459201L,
            mediaType = "IMAGE",
            size = 1024000L,
            width = 1920,
            height = 1080,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            data = "/storage/emulated/0/DCIM/Camera/photo.jpg",
            iso = 400,
            isFavorite = true,
            isDeleted = false,
            deletedAt = null,
            isHidden = false,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        assertEquals(123L, entity.id)
        assertEquals(1024000L, entity.size)
        assertEquals(1920, entity.width)
        assertEquals(1080, entity.height)
        assertEquals("image/jpeg", entity.mimeType)
        assertEquals("Camera", entity.bucketDisplayName)
        assertEquals(400, entity.iso)
        assertTrue(entity.isFavorite)
        assertFalse(entity.isDeleted)
        assertFalse(entity.isHidden)
    }

    @Test
    fun `test PhotoMetadataEntity with VIDEO type`() {
        val entity = PhotoMetadataEntity(
            id = 456L,
            uri = "content://media/external/video/media/456",
            displayName = "video.mp4",
            dateAdded = 1609459200L,
            dateModified = 1609459200L,
            mediaType = "VIDEO"
        )

        assertEquals("VIDEO", entity.mediaType)
        assertEquals("video.mp4", entity.displayName)
    }

    @Test
    fun `test PhotoMetadataEntity with deleted state`() {
        val deletedAt = System.currentTimeMillis() / 1000
        val entity = PhotoMetadataEntity(
            id = 789L,
            uri = "content://media/external/images/media/789",
            displayName = "deleted.jpg",
            dateAdded = 1609459200L,
            dateModified = 1609459200L,
            mediaType = "IMAGE",
            isDeleted = true,
            deletedAt = deletedAt
        )

        assertTrue(entity.isDeleted)
        assertEquals(deletedAt, entity.deletedAt)
    }

    @Test
    fun `test PhotoMetadataEntity with hidden state`() {
        val entity = PhotoMetadataEntity(
            id = 999L,
            uri = "content://media/external/images/media/999",
            displayName = "hidden.jpg",
            dateAdded = 1609459200L,
            dateModified = 1609459200L,
            mediaType = "IMAGE",
            isHidden = true
        )

        assertTrue(entity.isHidden)
    }
}
