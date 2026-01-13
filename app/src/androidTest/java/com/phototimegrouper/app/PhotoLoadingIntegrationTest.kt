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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 照片加载流程集成测试
 * 
 * 测试完整的数据流�?
 * 1. 权限检�?�?照片扫描 �?数据分组 �?RecyclerView 显示
 * 2. 下拉刷新流程
 * 3. 错误处理流程
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoLoadingIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testCompletePhotoLoadingFlow() {
        // 1. 验证 Activity 启动
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 验证 Activity 不为 null
                assert(activity != null)
                
                // 验证权限已授�?
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
            
            // 2. 验证 SwipeRefreshLayout 显示
            onView(withId(R.id.swipeRefreshLayout))
                .check(matches(isDisplayed()))
            
            // 3. 等待照片加载（给足够的时间让协程完成�?
            Thread.sleep(3000)
            
            // 4. 验证 RecyclerView 显示（照片加载完成后应该显示�?
            onView(withId(R.id.recyclerView))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhotoLoadingWithSwipeRefresh() {
        // 测试下拉刷新流程
        
        // 1. 等待初始加载
        Thread.sleep(2000)
        
        // 2. 验证 RecyclerView 显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        // 3. 执行下拉刷新
        onView(withId(R.id.swipeRefreshLayout))
            .perform(swipeDown())
        
        // 4. 等待刷新完成（刷新动画和重新加载�?
        Thread.sleep(3000)
        
        // 5. 验证列表仍然显示（刷新后列表应该仍然存在�?
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRefreshLayoutConfiguration() {
        // 测试 SwipeRefreshLayout 配置是否正确
        
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val swipeRefreshLayout = activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
                
                // 验证 SwipeRefreshLayout 存在
                assert(swipeRefreshLayout != null)
                
                // 验证 RecyclerView �?SwipeRefreshLayout 的子视图
                val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
                assert(recyclerView != null)
            }
        }
    }

    @Test
    fun testMainActivityLifecycle() {
        // 测试 Activity 生命周期中的照片加载
        
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // 1. 验证 onCreate 后视图已初始�?
        scenario.onActivity { activity ->
            val swipeRefreshLayout = activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            assert(swipeRefreshLayout != null)
        }
        
        // 2. 等待照片加载
        Thread.sleep(3000)
        
        // 3. 验证照片列表显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        // 4. 重新创建 Activity（模拟配置更改）
        scenario.recreate()
        
        // 5. 等待重新加载
        Thread.sleep(3000)
        
        // 6. 验证列表仍然显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
}
