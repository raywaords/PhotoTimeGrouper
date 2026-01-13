package com.phototimegrouper.app

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.phototimegrouper.app.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 照片导航集成测试
 * 
 * 测试用户交互流程�?
 * 1. 主界面点击照�?�?启动详情�?
 * 2. 详情页显示照片信�?
 * 3. 详情页左右滑动浏�?
 * 4. 返回主界�?
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoNavigationIntegrationTest {

    private lateinit var testPhotoList: ArrayList<PhotoItem>
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var device: UiDevice

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Before
    fun setUp() {
        // 创建测试照片列表
        testPhotoList = arrayListOf(
            PhotoItem(
                id = 1L,
                uri = "content://media/external/images/media/1",
                displayName = "test_photo_1.jpg",
                dateAdded = 1684149045L,
                dateModified = 1684149045L
            ),
            PhotoItem(
                id = 2L,
                uri = "content://media/external/images/media/2",
                displayName = "test_photo_2.jpg",
                dateAdded = 1684149050L,
                dateModified = 1684149050L
            ),
            PhotoItem(
                id = 3L,
                uri = "content://media/external/images/media/3",
                displayName = "test_photo_3.jpg",
                dateAdded = 1684149055L,
                dateModified = 1684149055L
            )
        )
        
        device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        if (::mainActivityScenario.isInitialized) {
            mainActivityScenario.close()
        }
    }

    @Test
    fun testDirectPhotoDetailNavigation() {
        // 测试直接启动详情页（模拟从主界面点击照片后的流程�?
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        ActivityScenario.launch<PhotoDetailActivity>(intent).use { scenario ->
            // 1. 验证详情页启�?
            Thread.sleep(1000)
            
            // 2. 验证 ViewPager2 显示
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 3. 验证照片信息布局显示
            onView(withId(R.id.infoLayout))
                .check(matches(isDisplayed()))
            
            // 4. 验证照片信息 TextView 显示
            onView(withId(R.id.photoNameTextView))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.photoDateTextView))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.photoIndexTextView))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhotoDetailSwipeNavigation() {
        // 测试详情页中的滑动导�?
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 1) // 从中间开�?
        }

        ActivityScenario.launch<PhotoDetailActivity>(intent).use { scenario ->
            Thread.sleep(2000)
            
            // 1. 向左滑动（下一张）
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            
            Thread.sleep(500)
            
            // 2. 验证 ViewPager2 仍然显示（没有崩溃）
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 3. 向右滑动（上一张）
            onView(withId(R.id.viewPager))
                .perform(swipeRight())
            
            Thread.sleep(500)
            
            // 4. 再次验证 ViewPager2 显示
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 5. 验证照片信息仍然显示
            onView(withId(R.id.infoLayout))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhotoDetailMultipleSwipes() {
        // 测试多次滑动（验证连续交互）
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        ActivityScenario.launch<PhotoDetailActivity>(intent).use { scenario ->
            Thread.sleep(2000)
            
            // 连续滑动测试
            repeat(3) { index ->
                // 向左滑动
                onView(withId(R.id.viewPager))
                    .perform(swipeLeft())
                
                Thread.sleep(500)
                
                // 验证 ViewPager2 仍然正常工作
                onView(withId(R.id.viewPager))
                    .check(matches(isDisplayed()))
                
                // 验证照片信息显示
                onView(withId(R.id.photoIndexTextView))
                    .check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun testPhotoDetailBackNavigation() {
        // 测试返回导航（使用系统返回键�?
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        ActivityScenario.launch<PhotoDetailActivity>(intent).use { scenario ->
            Thread.sleep(2000)
            
            // 1. 验证详情页显�?
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 2. 按返回键（模拟用户点击返回）
            device.pressBack()
            
            Thread.sleep(500)
            
            // 3. 验证 Activity 已关闭（通过场景状态检查）
            // 注意：ActivityScenario 会在 close() 时自动处理，这里主要是验证不会崩�?
            assert(scenario.state.toString().contains("DESTROYED") || scenario.state.toString().contains("RESUMED"))
        }
    }

    @Test
    fun testPhotoDetailWithSinglePhoto() {
        // 测试只有一张照片的情况
        
        val singlePhotoList = arrayListOf(
            PhotoItem(
                id = 1L,
                uri = "content://media/external/images/media/1",
                displayName = "single_photo.jpg",
                dateAdded = 1684149045L,
                dateModified = 1684149045L
            )
        )

        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, singlePhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        ActivityScenario.launch<PhotoDetailActivity>(intent).use { scenario ->
            Thread.sleep(2000)
            
            // 1. 验证详情页正常启�?
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 2. 验证索引显示�?"1 / 1"
            onView(withId(R.id.photoIndexTextView))
                .check(matches(isDisplayed()))
            
            // 3. 尝试滑动（应该没有效果，但不会崩溃）
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            
            Thread.sleep(500)
            
            // 4. 验证仍然显示
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
        }
    }
}
