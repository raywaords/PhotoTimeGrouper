package com.phototimegrouper.app.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * PhotoMetadataDao 集成测试
 * 使用 in-memory database 进行测试
 * 需要 Android 环境，放在 androidTest 目录
 */
@RunWith(AndroidJUnit4::class)
class PhotoMetadataDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PhotoMetadataDao

    @Before
    fun setup() {
        // 创建 in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.photoMetadataDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetPhotoById() = runBlocking {
        val entity = createTestEntity(1L, "test.jpg", "IMAGE")
        dao.insertPhoto(entity)

        val retrieved = dao.getPhotoById(1L)
        assertNotNull(retrieved)
        assertEquals("test.jpg", retrieved?.displayName)
        assertEquals("IMAGE", retrieved?.mediaType)
    }

    @Test
    fun testInsertMultiplePhotos() = runBlocking {
        val entities = listOf(
            createTestEntity(1L, "photo1.jpg", "IMAGE"),
            createTestEntity(2L, "photo2.jpg", "IMAGE"),
            createTestEntity(3L, "video1.mp4", "VIDEO")
        )
        dao.insertPhotos(entities)

        val allPhotos = dao.getAllPhotos().first()
        assertEquals(3, allPhotos.size)
    }

    @Test
    fun testGetFavoritePhotos() = runBlocking {
        val favorite1 = createTestEntity(1L, "favorite1.jpg", "IMAGE", isFavorite = true)
        val favorite2 = createTestEntity(2L, "favorite2.jpg", "IMAGE", isFavorite = true)
        val normal = createTestEntity(3L, "normal.jpg", "IMAGE", isFavorite = false)

        dao.insertPhotos(listOf(favorite1, favorite2, normal))

        val favorites = dao.getFavoritePhotos().first()
        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
    }

    @Test
    fun testSoftDeletePhoto() = runBlocking {
        val entity = createTestEntity(1L, "test.jpg", "IMAGE")
        dao.insertPhoto(entity)

        val deletedAt = System.currentTimeMillis() / 1000
        val updatedAt = deletedAt
        dao.softDeletePhoto(1L, deletedAt, updatedAt)

        val deleted = dao.getPhotoById(1L)
        assertNotNull(deleted)
        assertTrue(deleted!!.isDeleted)
        assertEquals(deletedAt, deleted.deletedAt)
    }

    @Test
    fun testRestoreDeletedPhoto() = runBlocking {
        val entity = createTestEntity(1L, "test.jpg", "IMAGE")
        dao.insertPhoto(entity)

        // 先删除
        val deletedAt = System.currentTimeMillis() / 1000
        dao.softDeletePhoto(1L, deletedAt, deletedAt)

        // 再恢复
        val updatedAt = System.currentTimeMillis() / 1000
        dao.restorePhoto(1L, updatedAt)

        val restored = dao.getPhotoById(1L)
        assertNotNull(restored)
        assertFalse(restored!!.isDeleted)
        assertNull(restored.deletedAt)
    }

    @Test
    fun testSetFavorite() = runBlocking {
        val entity = createTestEntity(1L, "test.jpg", "IMAGE", isFavorite = false)
        dao.insertPhoto(entity)

        val updatedAt = System.currentTimeMillis() / 1000
        dao.setFavorite(1L, true, updatedAt)

        val favorite = dao.isFavorite(1L)
        assertEquals(true, favorite)
    }

    @Test
    fun testSearchPhotosByName() = runBlocking {
        val entities = listOf(
            createTestEntity(1L, "sunset.jpg", "IMAGE"),
            createTestEntity(2L, "sunrise.jpg", "IMAGE"),
            createTestEntity(3L, "mountain.jpg", "IMAGE")
        )
        dao.insertPhotos(entities)

        val results = dao.searchPhotos("sun")
        assertEquals(2, results.size)
        assertTrue(results.all { it.displayName.contains("sun", ignoreCase = true) })
    }

    @Test
    fun testGetPhotosByDateRange() = runBlocking {
        val baseTime = 1609459200L // 2021-01-01
        val entities = listOf(
            createTestEntity(1L, "photo1.jpg", "IMAGE", dateAdded = baseTime),
            createTestEntity(2L, "photo2.jpg", "IMAGE", dateAdded = baseTime + 86400), // +1 day
            createTestEntity(3L, "photo3.jpg", "IMAGE", dateAdded = baseTime + 172800) // +2 days
        )
        dao.insertPhotos(entities)

        val results = dao.getPhotosByDateRange(baseTime, baseTime + 86400)
        assertEquals(2, results.size)
    }

    @Test
    fun testGetPhotosByMediaType() = runBlocking {
        val entities = listOf(
            createTestEntity(1L, "photo1.jpg", "IMAGE"),
            createTestEntity(2L, "photo2.jpg", "IMAGE"),
            createTestEntity(3L, "video1.mp4", "VIDEO")
        )
        dao.insertPhotos(entities)

        val images = dao.getPhotosByMediaType("IMAGE")
        assertEquals(2, images.size)
        assertTrue(images.all { it.mediaType == "IMAGE" })
    }

    @Test
    fun testGetAllPhotosExcludesDeletedAndHidden() = runBlocking {
        val normal = createTestEntity(1L, "normal.jpg", "IMAGE")
        val deleted = createTestEntity(2L, "deleted.jpg", "IMAGE", isDeleted = true)
        val hidden = createTestEntity(3L, "hidden.jpg", "IMAGE", isHidden = true)

        dao.insertPhotos(listOf(normal, deleted, hidden))

        val allPhotos = dao.getAllPhotos().first()
        assertEquals(1, allPhotos.size)
        assertEquals(1L, allPhotos[0].id)
    }

    @Test
    fun testDeleteOrphanedPhotos() = runBlocking {
        val entities = listOf(
            createTestEntity(1L, "photo1.jpg", "IMAGE"),
            createTestEntity(2L, "photo2.jpg", "IMAGE"),
            createTestEntity(3L, "photo3.jpg", "IMAGE")
        )
        dao.insertPhotos(entities)

        // 删除不在列表中的照片（保留 1 和 2）
        dao.deleteOrphanedPhotos(listOf(1L, 2L))

        val remaining = dao.getAllPhotos().first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.id in listOf(1L, 2L) })
    }

    // 辅助方法：创建测试实体
    private fun createTestEntity(
        id: Long,
        displayName: String,
        mediaType: String,
        dateAdded: Long = System.currentTimeMillis() / 1000,
        isFavorite: Boolean = false,
        isDeleted: Boolean = false,
        isHidden: Boolean = false
    ): PhotoMetadataEntity {
        return PhotoMetadataEntity(
            id = id,
            uri = "content://media/external/${if (mediaType == "IMAGE") "images" else "video"}/media/$id",
            displayName = displayName,
            dateAdded = dateAdded,
            dateModified = dateAdded,
            mediaType = mediaType,
            isFavorite = isFavorite,
            isDeleted = isDeleted,
            isHidden = isHidden
        )
    }
}
