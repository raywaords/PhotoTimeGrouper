package com.phototimegrouper.app

import android.os.Build
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PhotoItem Instrumented 测试（需�?Android 运行时环境）
 * 
 * 测试用例�?
 * 1. Parcelable 序列化和反序列化
 * 2. Parcelable 数组序列�?
 * 
 * 注意：这些测试需要在真实�?Android 设备或模拟器上运�?
 */
@RunWith(AndroidJUnit4::class)
class PhotoItemInstrumentedTest {

    @Test
    fun testPhotoItemParcelableWriteAndRead() {
        // 测试 Parcelable 序列化和反序列化
        val originalPhotoItem = PhotoItem(
            id = 67890L,
            uri = "content://media/external/images/media/67890",
            displayName = "test_photo.jpg",
            dateAdded = 1684149045L,
            dateModified = 1684150000L
        )

        // 创建 Parcel 并写�?
        val parcel = Parcel.obtain()
        originalPhotoItem.writeToParcel(parcel, 0)

        // 重置 parcel 位置
        parcel.setDataPosition(0)

        // �?parcel 读取（使用新�?API，兼�?Android 13+�?
        val recreatedPhotoItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(PhotoItem::class.java.classLoader, PhotoItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<PhotoItem>(PhotoItem::class.java.classLoader)
        }

        // 验证数据
        assertNotNull(recreatedPhotoItem)
        recreatedPhotoItem?.let {
            assertEquals(originalPhotoItem.id, it.id)
            assertEquals(originalPhotoItem.uri, it.uri)
            assertEquals(originalPhotoItem.displayName, it.displayName)
            assertEquals(originalPhotoItem.dateAdded, it.dateAdded)
            assertEquals(originalPhotoItem.dateModified, it.dateModified)
        }

        parcel.recycle()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testPhotoItemParcelableWriteAndReadAndroid13Plus() {
        // 专门测试 Android 13+ 的新 API
        val originalPhotoItem = PhotoItem(
            id = 67890L,
            uri = "content://media/external/images/media/67890",
            displayName = "test_photo.jpg",
            dateAdded = 1684149045L,
            dateModified = 1684150000L
        )

        val parcel = Parcel.obtain()
        originalPhotoItem.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val recreatedPhotoItem = parcel.readParcelable(PhotoItem::class.java.classLoader, PhotoItem::class.java)

        assertNotNull(recreatedPhotoItem)
        recreatedPhotoItem?.let {
            assertEquals(originalPhotoItem.id, it.id)
            assertEquals(originalPhotoItem.uri, it.uri)
            assertEquals(originalPhotoItem.displayName, it.displayName)
        }

        parcel.recycle()
    }

    @Test
    fun testPhotoItemParcelableArrayWriteAndReadArray() {
        // 测试 Parcelable 数组序列�?
        val photoItems = arrayOf(
            PhotoItem(
                id = 1L,
                uri = "content://media/external/images/media/1",
                displayName = "photo1.jpg",
                dateAdded = 1684149045L,
                dateModified = 1684149045L
            ),
            PhotoItem(
                id = 2L,
                uri = "content://media/external/images/media/2",
                displayName = "photo2.jpg",
                dateAdded = 1684149050L,
                dateModified = 1684149050L
            )
        )

        val parcel = Parcel.obtain()
        parcel.writeTypedArray(photoItems, 0)
        parcel.setDataPosition(0)

        // 使用 readParcelableArray 读取
        val recreatedArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelableArray(PhotoItem::class.java.classLoader, PhotoItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelableArray(PhotoItem::class.java.classLoader)
        }

        assertNotNull(recreatedArray)
        assertEquals(photoItems.size, recreatedArray?.size)
        
        recreatedArray?.let { array ->
            for (i in photoItems.indices) {
                val recreatedItem = array[i] as? PhotoItem
                assertNotNull(recreatedItem)
                recreatedItem?.let {
                    assertEquals(photoItems[i].id, it.id)
                    assertEquals(photoItems[i].uri, it.uri)
                    assertEquals(photoItems[i].displayName, it.displayName)
                    assertEquals(photoItems[i].dateAdded, it.dateAdded)
                    assertEquals(photoItems[i].dateModified, it.dateModified)
                }
            }
        }

        parcel.recycle()
    }
}