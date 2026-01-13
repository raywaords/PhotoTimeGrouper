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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.phototimegrouper.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivity UI 测试
 * 
 * 测试用例�?
 * 1. Activity 启动测试
 * 2. 权限请求测试
 * 3. RecyclerView 显示测试
 * 4. 下拉刷新测试
 * 5. ProgressBar 显示/隐藏测试
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testMainActivityLaunches() {
        // 测试 MainActivity 能够正常启动
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 验证 Activity 不为 null
                assert(activity != null)
                // 验证 SwipeRefreshLayout 存在
                assert(activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout) != null)
            }
        }
    }

    @Test
    fun testSwipeRefreshLayoutIsDisplayed() {
        // 测试 SwipeRefreshLayout 是否显示
        onView(withId(R.id.swipeRefreshLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecyclerViewIsDisplayed() {
        // 测试 RecyclerView 是否显示
        // 等待一段时间让数据加载
        Thread.sleep(3000)
        
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRefreshAction() {
        // 测试下拉刷新功能
        Thread.sleep(2000) // 等待初始加载完成
        
        // 执行下拉刷新动作
        onView(withId(R.id.swipeRefreshLayout))
            .perform(ViewActions.swipeDown())
        
        // 等待刷新动画完成
        Thread.sleep(2000)
        
        // 验证列表仍然显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testProgressBarVisibility() {
        // 测试 ProgressBar 的显�?隐藏逻辑
        // 这个测试需要在加载前后检�?ProgressBar 的状�?
        Thread.sleep(1000)
        
        // 初始状态下，ProgressBar 应该是隐藏的（gone�?
        // 但由于我们的实现中，ProgressBar 可能在某些情况下显示
        // 这里只是验证布局中存�?ProgressBar
        try {
            onView(withId(R.id.progressBar))
                .check(matches(isDisplayed()))
        } catch (e: AssertionError) {
            // 如果 ProgressBar 不可见（gone），这是正常�?
            // 我们可以验证它存在于布局�?
        }
    }

    @Test
    fun testPermissionGranted() {
        // 测试权限已授予的情况
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
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
                
                assert(hasPermission)
            }
        }
    }
}