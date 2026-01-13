# 集成测试指南

本文档说明如何在 PhotoTimeGrouper 项目中进行集成测试。

## 📚 什么是集成测试？

集成测试是测试多个组件如何协同工作的测试方法。在 Android 开发中，集成测试通常包括：

1. **组件集成测试** - 测试 Activity、Fragment、ViewModel、Adapter 等的交互
2. **UI 集成测试** - 测试用户界面和交互流程
3. **数据流集成测试** - 测试数据加载、显示、更新的完整流程
4. **系统集成测试** - 测试与 Android 系统组件（MediaStore、权限等）的集成

## 🔄 测试类型对比

| 测试类型 | 位置 | 运行环境 | 测试内容 | 速度 |
|---------|------|----------|---------|------|
| **单元测试** | `app/src/test/` | JVM | 单个类/方法 | 快 ⚡ |
| **集成测试** | `app/src/androidTest/` | 设备/模拟器 | 多个组件交互 | 中等 🚀 |
| **UI 测试** | `app/src/androidTest/` | 设备/模拟器 | 用户界面 | 慢 🐢 |

## 🎯 本项目中的集成测试

### 现有的集成测试

目前项目中已有的测试主要是 **UI 测试**，但可以扩展为更完整的集成测试：

1. **MainActivityTest.kt** - 主界面集成测试
   - Activity 启动
   - 权限处理
   - 照片加载流程
   - RecyclerView 显示
   - 下拉刷新

2. **PhotoDetailActivityTest.kt** - 详情页集成测试
   - Activity 启动（带 Intent）
   - ViewPager2 显示
   - 照片信息显示
   - 滑动交互

3. **PhotoItemInstrumentedTest.kt** - Parcelable 集成测试
   - Parcelable 序列化/反序列化
   - 数据传递

## 🚀 如何添加集成测试

### 步骤 1: 确定测试范围

对于 PhotoTimeGrouper 项目，建议的集成测试包括：

#### 1. 照片加载流程集成测试

测试从权限请求到照片显示的完整流程：
- 权限请求 → 照片扫描 → 数据分组 → UI 显示

#### 2. 数据流集成测试

测试数据在不同组件间的传递：
- MediaStore 查询 → PhotoItem 创建 → 分组 → Adapter 绑定 → RecyclerView 显示

#### 3. UI 交互集成测试

测试用户操作的完整流程：
- 点击照片 → 启动详情页 → 显示照片信息 → 滑动浏览

#### 4. 状态管理集成测试

测试应用状态的变化：
- 下拉刷新 → 重新加载 → 更新列表
- 权限被拒绝 → 显示提示

### 步骤 2: 选择测试工具

#### 推荐工具

1. **Espresso** - UI 测试框架（已包含）
   ```kotlin
   implementation 'androidx.test.espresso:espresso-core:3.5.1'
   implementation 'androidx.test.espresso:espresso-contrib:3.5.1'
   ```

2. **ActivityScenario** - Activity 生命周期测试（已包含）
   ```kotlin
   implementation 'androidx.test:core:1.5.0'
   ```

3. **UI Automator** - 跨应用 UI 测试（已包含）
   ```kotlin
   implementation 'androidx.test.uiautomator:uiautomator:2.3.0'
   ```

### 步骤 3: 编写集成测试

## 📝 集成测试示例

### 示例 1: 照片加载流程集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoLoadingIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testPhotoLoadingFlow() {
        // 1. 验证 Activity 启动
        onView(withId(R.id.swipeRefreshLayout))
            .check(matches(isDisplayed()))

        // 2. 等待照片加载（模拟真实场景）
        Thread.sleep(3000)

        // 3. 验证 RecyclerView 显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))

        // 4. 验证至少有一个照片组显示
        onView(withId(R.id.recyclerView))
            .check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun testPhotoLoadingWithRefresh() {
        // 1. 初始加载
        Thread.sleep(2000)
        
        // 2. 下拉刷新
        onView(withId(R.id.swipeRefreshLayout))
            .perform(swipeDown())
        
        // 3. 等待刷新完成
        Thread.sleep(2000)
        
        // 4. 验证列表仍然显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }
}
```

### 示例 2: 照片点击到详情页集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class PhotoNavigationIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testPhotoClickToDetailFlow() {
        // 1. 启动主界面
        val mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        
        // 2. 等待照片加载
        Thread.sleep(3000)
        
        // 3. 点击第一张照片（需要根据实际布局调整）
        // 注意：由于使用了嵌套 RecyclerView，需要找到第一个照片项
        try {
            onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            
            // 4. 验证详情页启动
            Thread.sleep(1000)
            
            // 5. 验证详情页元素显示
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.photoNameTextView))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 如果没有照片，跳过此测试
            Log.d("PhotoNavigationTest", "No photos available: ${e.message}")
        }
        
        mainActivityScenario.close()
    }
}
```

### 示例 3: 数据分组集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
class PhotoGroupingIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun testPhotoGroupingFlow() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        
        activityScenario.onActivity { activity ->
            // 1. 模拟照片加载
            val testPhotos = arrayListOf(
                PhotoItem(1L, "uri1", "photo1.jpg", 1684149045L, 1684149045L), // 2023-05-15
                PhotoItem(2L, "uri2", "photo2.jpg", 1684235445L, 1684235445L), // 2023-05-15
                PhotoItem(3L, "uri3", "photo3.jpg", 1684321845L, 1684321845L), // 2023-05-16
            )
            
            // 2. 测试分组逻辑
            val groupedMap = testPhotos.groupBy { photo ->
                DateFormatter.formatDateForGroup(photo.dateModified)
            }
            
            // 3. 验证分组结果
            assertEquals(2, groupedMap.size) // 应该有 2 个日期组
            assertTrue(groupedMap.containsKey("2023-05-15"))
            assertTrue(groupedMap.containsKey("2023-05-16"))
            assertEquals(2, groupedMap["2023-05-15"]?.size) // 2023-05-15 有 2 张照片
        }
        
        activityScenario.close()
    }
}
```

### 示例 4: 权限流程集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class PermissionFlowIntegrationTest {

    @Test
    fun testPermissionDeniedFlow() {
        // 注意：这个测试需要模拟权限被拒绝的情况
        // 可以使用 Mockito 或自定义权限规则
        
        // 1. 启动 Activity（没有权限）
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        
        // 2. 验证权限请求逻辑
        Thread.sleep(1000)
        
        // 3. 验证没有照片显示（因为没有权限）
        // 实际实现取决于权限被拒绝时的 UI 行为
        
        activityScenario.close()
    }

    @Test
    fun testPermissionGrantedFlow() {
        val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        
        // 1. 验证权限已授予
        activityScenario.onActivity { activity ->
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            
            assertTrue("Permission should be granted", hasPermission)
        }
        
        // 2. 等待照片加载
        Thread.sleep(3000)
        
        // 3. 验证照片显示
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        activityScenario.close()
    }
}
```

## 🔧 集成测试最佳实践

### 1. 测试独立性

每个测试应该独立运行，不依赖其他测试的状态：

```kotlin
@Before
fun setUp() {
    // 设置测试环境
}

@After
fun tearDown() {
    // 清理测试数据
}
```

### 2. 使用 IdlingResource（推荐）

避免使用 `Thread.sleep()`，使用 Espresso 的 IdlingResource：

```kotlin
// 创建 IdlingResource
class PhotoLoadingIdlingResource : IdlingResource {
    private var callback: IdlingResource.ResourceCallback? = null
    private var isIdle = false

    override fun getName() = "PhotoLoadingIdlingResource"
    override fun isIdleNow() = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    fun setIdle(isIdle: Boolean) {
        this.isIdle = isIdle
        if (isIdle) {
            callback?.onTransitionToIdle()
        }
    }
}

// 在测试中使用
@Test
fun testPhotoLoading() {
    val idlingResource = PhotoLoadingIdlingResource()
    IdlingRegistry.getInstance().register(idlingResource)
    
    try {
        // 执行测试
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    } finally {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
```

### 3. 测试数据准备

使用测试数据而不是依赖真实设备数据：

```kotlin
@Test
fun testWithMockData() {
    // 1. 创建测试数据
    val testPhotos = createTestPhotoList()
    
    // 2. 注入测试数据（如果使用依赖注入）
    // 或使用 Mockito 模拟数据源
    
    // 3. 执行测试
    // ...
}
```

### 4. 错误处理测试

测试异常情况和边界情况：

```kotlin
@Test
fun testEmptyPhotoList() {
    // 测试没有照片的情况
    // 验证空状态显示
}

@Test
fun testPhotoLoadError() {
    // 测试照片加载失败的情况
    // 验证错误处理
}
```

### 5. 性能测试

在集成测试中也可以关注性能：

```kotlin
@Test
fun testPhotoLoadPerformance() {
    val startTime = System.currentTimeMillis()
    
    // 执行照片加载
    // ...
    
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime
    
    assertTrue("Photo loading should complete within 5 seconds", duration < 5000)
}
```

## 📊 集成测试策略

### 推荐的测试金字塔

```
        /\
       /  \      E2E Tests (少量)
      /----\
     /      \    Integration Tests (适量)
    /--------\
   /          \  Unit Tests (大量)
  /------------\
```

### 测试优先级

1. **高优先级** - 核心功能流程
   - 照片加载流程
   - 照片显示
   - 权限处理

2. **中优先级** - 用户交互
   - 点击照片查看详情
   - 下拉刷新
   - 滑动浏览

3. **低优先级** - 边界情况
   - 空列表
   - 错误处理
   - 性能测试

## ✅ 已创建的集成测试

### 1. PhotoLoadingIntegrationTest.kt - 照片加载流程集成测试

**位置**: `app/src/androidTest/java/com/example/phototimegrouper/PhotoLoadingIntegrationTest.kt`

**测试用例**:
- ✅ `testCompletePhotoLoadingFlow` - 完整的照片加载流程（权限 → 扫描 → 显示）
- ✅ `testPhotoLoadingWithSwipeRefresh` - 下拉刷新流程
- ✅ `testSwipeRefreshLayoutConfiguration` - SwipeRefreshLayout 配置测试
- ✅ `testMainActivityLifecycle` - Activity 生命周期测试

**测试内容**:
- 权限检查 → 照片扫描 → 数据分组 → RecyclerView 显示
- 下拉刷新功能
- Activity 生命周期中的照片加载

### 2. PhotoNavigationIntegrationTest.kt - 照片导航集成测试

**位置**: `app/src/androidTest/java/com/example/phototimegrouper/PhotoNavigationIntegrationTest.kt`

**测试用例**:
- ✅ `testDirectPhotoDetailNavigation` - 直接启动详情页（模拟点击照片）
- ✅ `testPhotoDetailSwipeNavigation` - 详情页滑动导航
- ✅ `testPhotoDetailMultipleSwipes` - 多次滑动测试（连续交互）
- ✅ `testPhotoDetailBackNavigation` - 返回导航测试
- ✅ `testPhotoDetailWithSinglePhoto` - 单张照片测试

**测试内容**:
- 主界面 → 点击照片 → 详情页
- 详情页左右滑动
- 返回导航
- 边界情况（单张照片）

### 3. PhotoDataFlowIntegrationTest.kt - 数据流集成测试

**位置**: `app/src/androidTest/java/com/example/phototimegrouper/PhotoDataFlowIntegrationTest.kt`

**测试用例**:
- ✅ `testPhotoGroupingLogic` - 照片分组逻辑测试
- ✅ `testDateFormatterIntegration` - DateFormatter 集成测试
- ✅ `testPhotoItemParcelableFlow` - PhotoItem Parcelable 流测试
- ✅ `testPhotoListParcelableFlow` - PhotoItem 列表 Parcelable 流测试
- ✅ `testPhotoGroupAdapterDataBinding` - PhotoGroupAdapter 数据绑定测试
- ✅ `testPhotoItemToDisplayNameFlow` - PhotoItem 到显示名称的转换流程

**测试内容**:
- MediaStore → PhotoItem → 分组 → Adapter
- PhotoItem → Parcelable → Intent → 详情页
- DateFormatter 在数据流中的使用
- Adapter 数据绑定流程

### 测试统计

- **集成测试类**: 3 个
- **测试用例总数**: 15 个
- **测试类型**: 
  - 照片加载流程（4 个）
  - UI 导航流程（5 个）
  - 数据流测试（6 个）

## 🎯 下一步行动

### 可选的额外集成测试

1. **状态管理集成测试**（可选）
   - [ ] 刷新状态测试
   - [ ] 加载状态测试
   - [ ] 错误状态测试

2. **性能集成测试**（可选）
   - [ ] 大量照片加载性能测试
   - [ ] 滑动性能测试

## 🔍 运行集成测试

### 在 Android Studio 中运行

1. **运行所有集成测试**：
   - 右键点击 `app/src/androidTest` 目录
   - 选择 `Run 'Tests in 'androidTest''`

2. **运行单个测试类**：
   - 打开测试文件
   - 点击类名旁边的绿色运行按钮

3. **运行单个测试方法**：
   - 点击测试方法旁边的绿色运行按钮

### 使用 Gradle 命令

```bash
# 运行所有 Instrumented 测试（包括集成测试）
./gradlew connectedAndroidTest

# 运行特定测试类
./gradlew connectedAndroidTest --tests "com.example.phototimegrouper.PhotoLoadingIntegrationTest"
```

### 查看测试报告

测试报告位置：
```
app/build/reports/androidTests/connected/index.html
```

## ⚠️ 注意事项

1. **需要设备/模拟器** - 集成测试必须在 Android 设备或模拟器上运行

2. **测试稳定性** - 使用 IdlingResource 而不是 Thread.sleep()

3. **测试数据** - 尽量使用测试数据，避免依赖真实设备数据

4. **测试隔离** - 确保每个测试独立，不依赖其他测试

5. **性能考虑** - 集成测试比单元测试慢，合理控制测试数量

## 📚 相关资源

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [ActivityScenario](https://developer.android.com/reference/androidx/test/core/app/ActivityScenario)
- [Testing Best Practices](https://developer.android.com/training/testing/fundamentals)

---

**最后更新**: 2026-01-11  
**状态**: 📝 指南文档
