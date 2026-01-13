package com.phototimegrouper.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PhotoItem 单元测试（纯 Java/Kotlin 测试，不依赖 Android 框架�?
 * 
 * 测试用例�?
 * 1. 数据类属性正确�?
 * 2. 边界值测�?
 * 3. 相等性测�?
 * 
 * 注意：Parcelable 测试已移�?androidTest/PhotoItemInstrumentedTest.kt
 * 因为 Parcelable 需要真实的 Android 运行时环�?
 */
class PhotoItemTest {

    @Test
    fun `test PhotoItem creation - normal data`() {
        // 测试正常创建 PhotoItem
        val photoItem = PhotoItem(
            id = 12345L,
            uri = "content://media/external/images/media/12345",
            displayName = "IMG_20230515_143045.jpg",
            dateAdded = 1684149045L,
            dateModified = 1684149045L
        )

        assertEquals(12345L, photoItem.id)
        assertEquals("content://media/external/images/media/12345", photoItem.uri)
        assertEquals("IMG_20230515_143045.jpg", photoItem.displayName)
        assertEquals(1684149045L, photoItem.dateAdded)
        assertEquals(1684149045L, photoItem.dateModified)
    }

    @Test
    fun `test PhotoItem equals - same data`() {
        // 测试相等�?
        val photoItem1 = PhotoItem(
            id = 123L,
            uri = "content://test",
            displayName = "test.jpg",
            dateAdded = 1000L,
            dateModified = 1000L
        )

        val photoItem2 = PhotoItem(
            id = 123L,
            uri = "content://test",
            displayName = "test.jpg",
            dateAdded = 1000L,
            dateModified = 1000L
        )

        assertEquals(photoItem1, photoItem2)
        assertEquals(photoItem1.hashCode(), photoItem2.hashCode())
    }

    @Test
    fun `test PhotoItem equals - different data`() {
        // 测试不相等的情况
        val photoItem1 = PhotoItem(
            id = 123L,
            uri = "content://test1",
            displayName = "test1.jpg",
            dateAdded = 1000L,
            dateModified = 1000L
        )

        val photoItem2 = PhotoItem(
            id = 456L,
            uri = "content://test2",
            displayName = "test2.jpg",
            dateAdded = 2000L,
            dateModified = 2000L
        )

        assert(!photoItem1.equals(photoItem2))
    }

    @Test
    fun `test PhotoItem - boundary values`() {
        // 测试边界�?
        val photoItem = PhotoItem(
            id = 0L, // 最�?ID
            uri = "", // �?URI
            displayName = "", // 空名�?
            dateAdded = 0L, // 纪元时间
            dateModified = Long.MAX_VALUE // 最大时间戳
        )

        assertEquals(0L, photoItem.id)
        assertEquals("", photoItem.uri)
        assertEquals("", photoItem.displayName)
        assertEquals(0L, photoItem.dateAdded)
        assertEquals(Long.MAX_VALUE, photoItem.dateModified)
    }

    @Test
    fun `test PhotoItem - long display name`() {
        // 测试很长的文件名
        val longName = "a".repeat(255)
        val photoItem = PhotoItem(
            id = 1L,
            uri = "content://test",
            displayName = longName,
            dateAdded = 1000L,
            dateModified = 1000L
        )

        assertEquals(longName, photoItem.displayName)
        assertEquals(255, photoItem.displayName.length)
    }
}