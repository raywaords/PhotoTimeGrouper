package com.phototimegrouper.app

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.phototimegrouper.app.R
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ??????????
 * 
 * ??????????
 * 1. ??????????? ?????? ??RecyclerView ??
 * 2. ??????
 * 3. ??????
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore("Uses legacy MainActivity and swipeRefreshLayout/recyclerView; app now uses MainActivityNew + fragments")
class PhotoLoadingIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
    fun testCompletePhotoLoadingFlow() {
        // 1. ?? Activity ??
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // ?? Activity ?? null
                assert(activity != null)
                
                // ????????
                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
                
                assertTrue("Permission should be granted", hasPermission)
            }
            
            // 2. ?? SwipeRefreshLayout ??
            onView(withId(R.id.swipeRefreshLayout))
                .check(matches(isDisplayed()))
            
            // 3. ????????????????????
            Thread.sleep(3000)
            
            // 4. ?? RecyclerView ????????????????
            onView(withId(R.id.recyclerView))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhotoLoadingWithSwipeRefresh() {
        // ????????
        
        // 1. ??????
        Thread.sleep(2000)
        
        // 2. ?? RecyclerView ??
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        // 3. ??????
        onView(withId(R.id.swipeRefreshLayout))
            .perform(swipeDown())
        
        // 4. ??????????????????
        Thread.sleep(3000)
        
        // 5. ??????????????????????
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRefreshLayoutConfiguration() {
        // ?? SwipeRefreshLayout ??????
        
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val swipeRefreshLayout = activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
                
                // ?? SwipeRefreshLayout ??
                assert(swipeRefreshLayout != null)
                
                // ?? RecyclerView ??SwipeRefreshLayout ????
                val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
                assert(recyclerView != null)
            }
        }
    }

    @Test
    fun testMainActivityLifecycle() {
        // ?? Activity ??????????
        
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // 1. ?? onCreate ????????
        scenario.onActivity { activity ->
            val swipeRefreshLayout = activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            assert(swipeRefreshLayout != null)
        }
        
        // 2. ??????
        Thread.sleep(3000)
        
        // 3. ????????
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        // 4. ???? Activity????????
        scenario.recreate()
        
        // 5. ??????
        Thread.sleep(3000)
        
        // 6. ????????
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}
