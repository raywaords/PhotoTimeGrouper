package com.phototimegrouper.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Locale

/**
 * 数据流集成测�?
 * 
 * 测试数据在不同组件间的传递：
 * 1. MediaStore 查询 �?PhotoItem 创建 �?数据分组 �?Adapter 绑定 �?RecyclerView 显示
 * 2. PhotoItem �?Parcelable �?Intent �?详情�?
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoDataFlowIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun createTestPhotoList(): ArrayList<PhotoItem> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        
        return arrayListOf(
            PhotoItem(
                id = 1L,
                uri = "content://media/external/images/media/1",
                displayName = "photo_2023_05_15_001.jpg",
                dateAdded = 1684149045L, // 2023-05-15 14:30:45
                dateModified = 1684149045L
            ),
            PhotoItem(
                id = 2L,
                uri = "content://media/external/images/media/2",
                displayName = "photo_2023_05_15_002.jpg",
                dateAdded = 1684149050L, // 2023-05-15 14:30:50
                dateModified = 1684149050L
            ),
            PhotoItem(
                id = 3L,
                uri = "content://media/external/images/media/3",
                displayName = "photo_2023_05_16_001.jpg",
                dateAdded = 1684235445L, // 2023-05-16 14:30:45
                dateModified = 1684235445L
            ),
            PhotoItem(
                id = 4L,
                uri = "content://media/external/images/media/4",
                displayName = "photo_2023_05_16_002.jpg",
                dateAdded = 1684235450L, // 2023-05-16 14:30:50
                dateModified = 1684235450L
            )
        )
    }

    @Test
    fun testPhotoGroupingLogic() {
        // 测试照片分组逻辑（DateFormatter + PhotoItem�?
        
        val testPhotos = createTestPhotoList()
        
        // 1. 使用 DateFormatter 进行分组（模�?MainActivity 的分组逻辑�?
        val groupedMap = testPhotos.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
        
        // 2. 验证分组结果
        assertNotNull(groupedMap)
        assertEquals(2, groupedMap.size) // 应该�?2 个日期组
        
        // 3. 验证日期组存�?
        assertTrue("Should contain 2023-05-15", groupedMap.containsKey("2023-05-15"))
        assertTrue("Should contain 2023-05-16", groupedMap.containsKey("2023-05-16"))
        
        // 4. 验证每个日期组的照片数量
        val group20230515 = groupedMap["2023-05-15"]
        assertNotNull(group20230515)
        assertEquals(2, group20230515?.size) // 2023-05-15 �?2 张照�?
        
        val group20230516 = groupedMap["2023-05-16"]
        assertNotNull(group20230516)
        assertEquals(2, group20230516?.size) // 2023-05-16 �?2 张照�?
    }

    @Test
    fun testDateFormatterIntegration() {
        // 测试 DateFormatter 在数据流中的使用
        
        val testPhotos = createTestPhotoList()
        
        // 1. 测试日期分组格式�?
        testPhotos.forEach { photo ->
            val dateString = DateFormatter.formatDateForGroup(photo.dateModified)
            assertNotNull(dateString)
            assertTrue("Date string should match format yyyy-MM-dd", dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        }
        
        // 2. 测试日期时间格式�?
        testPhotos.forEach { photo ->
            val dateTimeString = DateFormatter.formatDateTime(photo.dateModified)
            assertNotNull(dateTimeString)
            assertTrue("DateTime string should contain date and time", dateTimeString.contains("2023"))
        }
        
        // 3. 测试日期标题格式�?
        val groupedMap = testPhotos.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
        
        groupedMap.keys.forEach { dateString ->
            val formattedHeader = DateFormatter.formatDateHeader(dateString)
            assertNotNull(formattedHeader)
            assertTrue("Formatted header should not be empty", formattedHeader.isNotEmpty())
        }
    }

    @Test
    fun testPhotoItemParcelableFlow() {
        // 测试 PhotoItem 通过 Parcelable �?Activity 间传�?
        
        val testPhotos = createTestPhotoList()
        val originalPhoto = testPhotos[0]
        
        // 1. 测试单个 PhotoItem 序列�?
        val parcel = android.os.Parcel.obtain()
        originalPhoto.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        
        val recreatedPhoto = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(PhotoItem::class.java.classLoader, PhotoItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<PhotoItem>(PhotoItem::class.java.classLoader)
        }
        
        assertNotNull(recreatedPhoto)
        recreatedPhoto?.let {
            assertEquals(originalPhoto.id, it.id)
            assertEquals(originalPhoto.uri, it.uri)
            assertEquals(originalPhoto.displayName, it.displayName)
            assertEquals(originalPhoto.dateAdded, it.dateAdded)
            assertEquals(originalPhoto.dateModified, it.dateModified)
        }
        
        parcel.recycle()
    }

    @Test
    fun testPhotoListParcelableFlow() {
        // 测试 PhotoItem 列表通过 Intent 传�?
        
        val testPhotos = createTestPhotoList()
        
        // 1. 创建 Intent（模�?MainActivity 启动 PhotoDetailActivity�?
        val intent = android.content.Intent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            PhotoDetailActivity::class.java
        ).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotos)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }
        
        // 2. �?Intent 读取数据（模�?PhotoDetailActivity 接收�?
        val photoList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, PhotoItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<PhotoItem>(PhotoDetailActivity.EXTRA_PHOTO_LIST)
        }
        
        assertNotNull(photoList)
        assertEquals(testPhotos.size, photoList?.size)
        
        // 3. 验证数据完整�?
        if (photoList != null) {
            for (i in testPhotos.indices) {
                val original = testPhotos[i]
                val received = photoList[i]
                
                assertEquals(original.id, received.id)
                assertEquals(original.uri, received.uri)
                assertEquals(original.displayName, received.displayName)
                assertEquals(original.dateAdded, received.dateAdded)
                assertEquals(original.dateModified, received.dateModified)
            }
        }
    }

    @Test
    fun testPhotoGroupAdapterDataBinding() {
        // 测试 PhotoGroupAdapter 的数据绑定流�?
        
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val testPhotos = createTestPhotoList()
        
        // 1. 模拟分组（MainActivity 的逻辑�?
        val groupedMap = testPhotos.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
        
        // 2. 创建 Adapter（模�?MainActivity 创建 Adapter�?
        val adapter = PhotoGroupAdapter(
            context = context,
            groupedPhotos = groupedMap,
            onPhotoClick = { photoList, position ->
                // 点击回调（模拟跳转到详情页）
            }
        )
        
        // 3. 验证 Adapter 数据
        assertNotNull(adapter)
        assertEquals(groupedMap.size, adapter.itemCount)
        
        // 4. 验证日期排序（应该按降序�?
        // 注意：PhotoGroupAdapter 内部使用 sortedDescending()，最新的日期在前
        // 这里我们验证分组数量是否正确
        assertTrue("Adapter should have items", adapter.itemCount > 0)
    }

    @Test
    fun testPhotoItemToDisplayNameFlow() {
        // 测试 PhotoItem 数据到显示名称的转换流程
        
        val testPhotos = createTestPhotoList()
        
        testPhotos.forEach { photo ->
            // 1. PhotoItem �?displayName
            assertNotNull(photo.displayName)
            assertTrue("Display name should not be empty", photo.displayName.isNotEmpty())
            
            // 2. 日期格式�?
            val dateTimeString = DateFormatter.formatDateTime(photo.dateModified)
            assertNotNull(dateTimeString)
            
            // 3. 日期分组
            val dateGroup = DateFormatter.formatDateForGroup(photo.dateModified)
            assertNotNull(dateGroup)
        }
    }
}
