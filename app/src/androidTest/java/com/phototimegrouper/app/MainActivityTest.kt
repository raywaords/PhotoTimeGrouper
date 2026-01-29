package com.phototimegrouper.app

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.phototimegrouper.app.R
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivity UI ??
 * 
 * ??????
 * 1. Activity ????
 * 2. ??????
 * 3. RecyclerView ????
 * 4. ??????
 * 5. ProgressBar ??/????
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivityNew::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testMainActivityLaunches() {
        // ?? MainActivity ??????
        ActivityScenario.launch(MainActivityNew::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
                assert(activity.findViewById<android.view.View>(R.id.fragmentContainer) != null)
            }
        }
    }

    @Test
    fun testFragmentContainerIsDisplayed() {
        onView(withId(R.id.fragmentContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationIsDisplayed() {
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    @Ignore("Old MainActivity layout; new UI uses MainActivityNew with fragments")
    fun testRecyclerViewIsDisplayed_removed() {
        // ?? RecyclerView ????
        // ???????????
        Thread.sleep(3000)
        
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    @Ignore("Old MainActivity layout; new UI uses MainActivityNew with fragments")
    fun testSwipeRefreshAction_skip() {
        // ????????
        Thread.sleep(2000) // ????????
        
        // ????????
        onView(withId(R.id.swipeRefreshLayout))
            .perform(ViewActions.swipeDown())
        
        // ????????
        Thread.sleep(2000)
        
        // ????????
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    @Ignore("Old MainActivity layout; new UI uses MainActivityNew with fragments")
    fun testProgressBarVisibility() {
        // ?? ProgressBar ????????
        // ??????????????ProgressBar ????
        Thread.sleep(1000)
        
        // ??????ProgressBar ???????gone??
        // ??????????ProgressBar ??????????
        // ????????????ProgressBar
        try {
            onView(withId(R.id.progressBar))
                .check(matches(isDisplayed()))
        } catch (e: AssertionError) {
            // ?? ProgressBar ????gone????????
            // ??????????????
        }
    }

    @Test
    fun testPermissionGranted() {
        // ??????????
        ActivityScenario.launch(MainActivityNew::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
                
                assert(hasPermission)
            }
        }
    }
}