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
 * PhotoDetailActivity UI ??
 * 
 * ??????
 * 1. Activity ?????? Intent??
 * 2. ViewPager2 ????
 * 3. ??????????????????
 * 4. ??????
 * 5. ??????
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoDetailActivityTest {

    private lateinit var testPhotoList: ArrayList<PhotoItem>
    private lateinit var scenario: ActivityScenario<PhotoDetailActivity>

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Before
    fun setUp() {
        // ????????
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

    /** ?? Application ????????? PhotoDetailActivity????? Application ?????? Intent? */
    private fun launchPhotoDetail(position: Int = 0, photos: ArrayList<PhotoItem> = testPhotoList) {
        (ApplicationProvider.getApplicationContext() as PhotoTimeGrouperApp).setAllPhotosList(photos)
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
        }
        scenario = ActivityScenario.launch(intent)
    }

    @Test
    fun testPhotoDetailActivityLaunches() {
        launchPhotoDetail()
        scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testViewPager2IsDisplayed() {
        launchPhotoDetail()
        
        // ?? ViewPager2 ????
        Thread.sleep(1000)
        
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPhotoInfoDisplayed() {
        launchPhotoDetail()
        Thread.sleep(2000)
        onView(withId(R.id.infoLayout))
            .check(matches(isDisplayed()))
        
        // ?????? TextView ??
        onView(withId(R.id.photoNameTextView))
            .check(matches(isDisplayed()))
        
        // ?????? TextView ??
        onView(withId(R.id.photoDateTextView))
            .check(matches(isDisplayed()))
        
        // ?????? TextView ??
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPhotoIndexDisplay() {
        launchPhotoDetail()
        Thread.sleep(2000)
        
        // ?????????????????????TextView ????
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeLeft() {
        launchPhotoDetail()
        Thread.sleep(2000)
        
        // ??????
        onView(withId(R.id.viewPager))
            .perform(swipeLeft())
        
        // ????????
        Thread.sleep(500)
        
        // ?? ViewPager2 ??????????
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRight() {
        launchPhotoDetail(position = 1)
        Thread.sleep(2000)
        
        // ??????
        onView(withId(R.id.viewPager))
            .perform(swipeRight())
        
        // ????????
        Thread.sleep(500)
        
        // ?? ViewPager2 ????
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleSwipes() {
        launchPhotoDetail()
        Thread.sleep(2000)
        
        // ??????
        repeat(3) {
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            Thread.sleep(500)
        }
        
        // ?? ViewPager2 ??????
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityWithSinglePhoto() {
        // ???????????
        val singlePhotoList = arrayListOf(
            PhotoItem(
                id = 1L,
                uri = "content://media/external/images/media/1",
                displayName = "single_photo.jpg",
                dateAdded = 1684149045L,
                dateModified = 1684149045L
            )
        )

        launchPhotoDetail(photos = singlePhotoList)
        Thread.sleep(2000)
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
        
        // ????????"1 / 1"
        onView(withId(R.id.photoIndexTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testActivityWithEmptyList() {
        val emptyList = arrayListOf<PhotoItem>()
        launchPhotoDetail(photos = emptyList)
        Thread.sleep(500)
    }
}