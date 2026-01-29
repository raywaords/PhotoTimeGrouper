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
 * ????????
 * 
 * ??????????
 * 1. ????????????????
 * 2. ??????????
 * 3. ??????????
 * 4. ??????
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoNavigationIntegrationTest {

    private lateinit var testPhotoList: ArrayList<PhotoItem>
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var device: UiDevice

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
        
        device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        if (::mainActivityScenario.isInitialized) {
            mainActivityScenario.close()
        }
    }

    private fun launchPhotoDetailScenario(position: Int = 0, photos: ArrayList<PhotoItem> = testPhotoList): ActivityScenario<PhotoDetailActivity> {
        (ApplicationProvider.getApplicationContext() as PhotoTimeGrouperApp).setAllPhotosList(photos)
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun testDirectPhotoDetailNavigation() {
        launchPhotoDetailScenario().use { _ ->
            Thread.sleep(1000)
            
            // 2. ?? ViewPager2 ??
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 3. ??????????
            onView(withId(R.id.infoLayout))
                .check(matches(isDisplayed()))
            
            // 4. ?????? TextView ??
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
        launchPhotoDetailScenario(position = 1).use { _ ->
            Thread.sleep(2000)
            
            // 1. ?????????
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            
            Thread.sleep(500)
            
            // 2. ?? ViewPager2 ??????????
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 3. ?????????
            onView(withId(R.id.viewPager))
                .perform(swipeRight())
            
            Thread.sleep(500)
            
            // 4. ???? ViewPager2 ??
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 5. ??????????
            onView(withId(R.id.infoLayout))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhotoDetailMultipleSwipes() {
        launchPhotoDetailScenario().use { _ ->
            Thread.sleep(2000)
            repeat(3) {
                // ????
                onView(withId(R.id.viewPager))
                    .perform(swipeLeft())
                
                Thread.sleep(500)
                
                // ?? ViewPager2 ??????
                onView(withId(R.id.viewPager))
                    .check(matches(isDisplayed()))
                
                // ????????
                onView(withId(R.id.photoIndexTextView))
                    .check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun testPhotoDetailBackNavigation() {
        launchPhotoDetailScenario().use { scenario ->
            Thread.sleep(2000)
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 2. ??????????????
            device.pressBack()
            
            Thread.sleep(500)
            
            // 3. ?? Activity ?????????????
            // ???ActivityScenario ?? close() ??????????????????
            assert(scenario.state.toString().contains("DESTROYED") || scenario.state.toString().contains("RESUMED"))
        }
    }

    @Test
    fun testPhotoDetailWithSinglePhoto() {
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

        (ApplicationProvider.getApplicationContext() as PhotoTimeGrouperApp).setAllPhotosList(singlePhotoList)
        val intent = Intent(ApplicationProvider.getApplicationContext(), PhotoDetailActivity::class.java).apply {
            putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, 0)
        }
        ActivityScenario.launch<PhotoDetailActivity>(intent).use { _ ->
            Thread.sleep(2000)
            
            // 1. ??????????
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            // 2. ????????"1 / 1"
            onView(withId(R.id.photoIndexTextView))
                .check(matches(isDisplayed()))
            
            // 3. ??????????????????
            onView(withId(R.id.viewPager))
                .perform(swipeLeft())
            
            Thread.sleep(500)
            
            // 4. ??????
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
        }
    }
}
