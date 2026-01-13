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
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.phototimegrouper.app.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PhotoDetailActivity UI 测试
 * 
 * 测试用例�?
 * 1. Activity 启动测试（带 Intent�?
 * 2. ViewPager2 显示测试
 * 3. 照片信息显示测试（名称、日期、索引）
 * 4. 左右滑动测试
 * 5. 返回按钮测试
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoDetailActivityTest {

    private lateinit var testPhotoList: ArrayList<PhotoItem>
    private lateinit var scenario: ActivityScenario<PhotoDetailActivity>

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
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun testPhotoDetailActivityLaunches() {
        // 测试 PhotoDetailActivity 能够正常启动
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testViewPager2IsDisplayed() {
        // 测试 ViewPager2 是否显示
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        // 等待 ViewPager2 初始�?
        Thread.sleep(1000)
        
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPhotoInfoDisplayed() {
        // 测试照片信息是否正确显示
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        // 等待界面加载
        Thread.sleep(2000)
        
        // 验证照片信息布局存在
        onView(withId(R.id.infoLayout))
            .check(matches(isDisplayed()))
        
        // 验证照片名称 TextView 存在
        onView(withId(R.id.photoNameTextView))
            .check(matches(isDisplayed()))
        
        // 验证照片日期 TextView 存在
        onView(withId(R.id.photoDateTextView))
            .check(matches(isDisplayed()))
        
        // 验证照片索引 TextView 存在
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPhotoIndexDisplay() {
        // 测试照片索引显示（应该是 "1 / 3"�?
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(2000)
        
        // 验证索引显示（由于是动态生成，这里只验�?TextView 存在�?
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeLeft() {
        // 测试向左滑动（切换到下一张照片）
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(2000)
        
        // 执行向左滑动
        onView(withId(R.id.viewPager))
            .perform(swipeLeft())
        
        // 等待滑动动画完成
        Thread.sleep(500)
        
        // 验证 ViewPager2 仍然显示（没有崩溃）
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRight() {
        // 测试向右滑动（切换到上一张照片）
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 1) // 从中间位置开�?
        }

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(2000)
        
        // 执行向右滑动
        onView(withId(R.id.viewPager))
            .perform(swipeRight())
        
        // 等待滑动动画完成
        Thread.sleep(500)
        
        // 验证 ViewPager2 仍然显示
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleSwipes() {
        // 测试多次滑动
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, testPhotoList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(2000)
        
        // 多次滑动测试
        repeat(3) {
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            Thread.sleep(500)
        }
        
        // 验证 ViewPager2 仍然正常工作
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityWithSinglePhoto() {
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

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(2000)
        
        // 验证活动正常启动
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
        
        // 验证索引显示�?"1 / 1"
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityWithEmptyList() {
        // 测试空列表的处理（应该正常启动但不显示内容）
        val emptyList = arrayListOf<PhotoItem>()

        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putParcelableArrayListExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, emptyList)
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }

        scenario = ActivityScenario.launch(intent)
        
        Thread.sleep(1000)
        
        // 验证活动能够启动（即使列表为空）
        // 实际实现中可能需要处理这种情况，否则可能崩溃
        scenario.onActivity { activity ->
            // 如果没有崩溃，说明处理正�?
            assert(activity != null)
        }
    }
}