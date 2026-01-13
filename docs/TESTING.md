# 测试文档

本文档说明如何运行 PhotoTimeGrouper 应用的测试用例。

## 📋 测试概览

本项目包含两种类型的测试：

1. **单元测试（Unit Tests）** - 测试独立的工具类和数据结构
2. **UI 测试（Instrumented Tests）** - 测试 Android Activity 和 UI 组件

## 🧪 测试结构

### 单元测试（`app/src/test/`）

**注意**：单元测试只包含不依赖 Android 框架的测试（纯 Java/Kotlin 测试）。需要 Android 运行时的测试（如 Parcelable）已移至 `androidTest`。

- `DateFormatterTest.kt` - 日期格式化工具类测试
  - ✅ 日期分组格式化
  - ✅ 日期时间格式化
  - ✅ 日期标题格式化
  - ✅ 线程安全性
  - ✅ 边界值测试
  - ✅ 异常处理

- `PhotoItemTest.kt` - 照片数据类单元测试（纯 Java/Kotlin，不依赖 Android）
  - ✅ 数据类属性正确性
  - ✅ 相等性测试
  - ✅ 边界值测试
  - ⚠️ 注意：Parcelable 测试已移至 `androidTest/PhotoItemInstrumentedTest.kt`

### UI 测试（`app/src/androidTest/`）

- `PhotoItemInstrumentedTest.kt` - PhotoItem Parcelable 测试（需要 Android 运行时）
  - ✅ Parcelable 序列化/反序列化
  - ✅ Parcelable 数组序列化
  - ✅ Android 13+ API 兼容性测试
  - ⚠️ 需要在设备/模拟器上运行

- `MainActivityTest.kt` - 主界面测试
  - ✅ Activity 启动测试
  - ✅ 权限请求测试
  - ✅ RecyclerView 显示测试
  - ✅ 下拉刷新功能测试
  - ✅ ProgressBar 显示/隐藏测试

- `PhotoDetailActivityTest.kt` - 照片详情页测试
  - ✅ Activity 启动测试（带 Intent）
  - ✅ ViewPager2 显示测试
  - ✅ 照片信息显示测试
  - ✅ 左右滑动测试
  - ✅ 多次滑动测试
  - ✅ 边界情况测试（单张照片、空列表）

## 🚀 运行测试

### 前置要求

1. **Android Studio** - 已安装并配置
2. **Android SDK** - 至少安装了 API 24+
3. **设备或模拟器** - UI 测试和 Instrumented 测试需要在设备上运行（纯单元测试不需要）

### 运行所有单元测试

**方法 1：使用 Android Studio**
1. 在项目视图中，右键点击 `app/src/test` 目录
2. 选择 `Run 'Tests in 'test''`
3. 查看测试结果

**方法 2：使用 Gradle 命令**
```bash
# Windows
gradlew.bat test

# Linux/Mac
./gradlew test
```

**方法 3：使用 Android Studio 终端**
在 Android Studio 底部的 Terminal 中运行：
```bash
./gradlew test
```

### 运行所有 UI 测试

**方法 1：使用 Android Studio**
1. 连接 Android 设备或启动模拟器
2. 在项目视图中，右键点击 `app/src/androidTest` 目录
3. 选择 `Run 'Tests in 'androidTest''`
4. 选择目标设备
5. 查看测试结果

**方法 2：使用 Gradle 命令**
```bash
# Windows
gradlew.bat connectedAndroidTest

# Linux/Mac
./gradlew connectedAndroidTest
```

**注意**：运行 UI 测试前，需要先连接 Android 设备或启动模拟器。

### 运行单个测试类

**使用 Android Studio：**
1. 打开测试文件（如 `DateFormatterTest.kt`）
2. 点击类名旁边的绿色运行图标
3. 选择 `Run 'DateFormatterTest'`

**使用 Gradle 命令：**
```bash
# 运行单个单元测试类
./gradlew test --tests "com.example.phototimegrouper.DateFormatterTest"

# 运行单个 UI 测试类
./gradlew connectedAndroidTest --tests "com.example.phototimegrouper.MainActivityTest"
```

### 运行单个测试方法

**使用 Android Studio：**
1. 打开测试文件
2. 点击测试方法旁边的绿色运行图标
3. 选择 `Run 'testMethodName()'`

**使用 Gradle 命令：**
```bash
# 运行单个测试方法
./gradlew test --tests "com.example.phototimegrouper.DateFormatterTest.test formatDateForGroup - normal timestamp"
```

## 📊 查看测试报告

### 单元测试报告

测试运行完成后，报告位置：
```
app/build/reports/tests/test/index.html
```

在浏览器中打开此文件查看详细的测试报告。

### UI 测试报告

UI 测试报告位置：
```
app/build/reports/androidTests/connected/index.html
```

## 🔍 测试覆盖率

查看代码覆盖率：

1. **运行测试时启用覆盖率：**
   - 在 Android Studio 中，右键点击测试类或方法
   - 选择 `Run 'TestName' with Coverage`

2. **查看覆盖率报告：**
   - 测试运行完成后，点击 `Coverage` 标签页
   - 查看各个文件的覆盖率百分比

3. **使用 Gradle 生成覆盖率报告：**
   ```bash
   ./gradlew testDebugUnitTest jacocoTestReport
   ```
   
   报告位置：`app/build/reports/jacoco/jacocoTestReport/html/index.html`

## 🐛 常见问题

### 1. 测试找不到类

**问题**：`ClassNotFoundException` 或 `NoClassDefFoundError`

**解决方案**：
- 确保已同步 Gradle（File → Sync Project with Gradle Files）
- 清理并重新构建项目（Build → Clean Project，然后 Build → Rebuild Project）

### 2. UI 测试无法连接到设备

**问题**：`No devices found`

**解决方案**：
- 检查设备是否已连接：`adb devices`
- 确保已启用 USB 调试
- 尝试重启 ADB：`adb kill-server && adb start-server`

### 3. 权限测试失败

**问题**：权限相关测试失败

**解决方案**：
- UI 测试使用 `GrantPermissionRule` 自动授予权限
- 确保测试设备上允许安装测试应用

### 4. 测试运行缓慢

**问题**：UI 测试运行很慢

**解决方案**：
- 使用模拟器而不是真机（模拟器通常更快）
- 减少 `Thread.sleep()` 的等待时间（如果有的话）
- 使用 Espresso 的 `IdlingResource` 而不是固定的等待时间

### 5. Mockito 相关错误

**问题**：`MockitoException` 或 `UnsupportedOperationException`

**解决方案**：
- 确保使用了正确的 Mockito 版本
- 检查是否使用了 `mockito-kotlin` 来处理 Kotlin 的 null 安全特性

### 6. Parcelable 测试相关问题

**问题**：`Parcel not mocked` 或 `Robolectric ShadowParcel error`

**解决方案**：
- **Parcelable 测试应该在 `androidTest`（Instrumented 测试）中运行**，而不是 `test`（单元测试）
- 我们已经将 Parcelable 测试移到了 `PhotoItemInstrumentedTest.kt`
- 运行 Parcelable 测试需要使用：`./gradlew connectedAndroidTest`（需要连接设备/模拟器）
- 单元测试（`test`）只包含不依赖 Android 框架的测试（如相等性、边界值等）

## 📝 编写新测试

### 单元测试示例

```kotlin
class MyClassTest {
    @Test
    fun `test my method`() {
        // Arrange（准备）
        val input = "test"
        
        // Act（执行）
        val result = MyClass.myMethod(input)
        
        // Assert（断言）
        assertEquals("expected", result)
    }
}
```

### UI 测试示例

```kotlin
@RunWith(AndroidJUnit4::class)
class MyActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MyActivity::class.java)
    
    @Test
    fun testMyButton() {
        onView(withId(R.id.myButton))
            .perform(click())
            .check(matches(isDisplayed()))
    }
}
```

## 🎯 最佳实践

1. **测试命名**：使用描述性的测试方法名，说明测试的内容
   ```kotlin
   // ✅ 好的命名
   fun `test formatDateForGroup - normal timestamp`()
   
   // ❌ 不好的命名
   fun test1()
   ```

2. **测试独立性**：每个测试应该独立运行，不依赖其他测试的状态

3. **使用 `@Before` 和 `@After`**：设置和清理测试环境

4. **避免硬编码等待**：使用 `IdlingResource` 或 `CountDownLatch` 而不是 `Thread.sleep()`

5. **测试边界情况**：不仅要测试正常情况，还要测试边界值和异常情况

## 📚 相关资源

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)

## 📅 测试状态

### ✅ 已完成的测试

**单元测试** (`app/src/test/`):
- [x] DateFormatter 单元测试（12 个测试用例）
- [x] PhotoItem 单元测试（5 个测试用例 - 不依赖 Android 框架）

**集成测试** (`app/src/androidTest/`):
- [x] PhotoItem Instrumented 测试（3 个测试用例 - Parcelable 测试）
- [x] MainActivity UI 测试（6 个测试用例）
- [x] PhotoDetailActivity UI 测试（9 个测试用例）
- [x] **PhotoLoadingIntegrationTest**（4 个测试用例 - 照片加载流程）✨ 新增
- [x] **PhotoNavigationIntegrationTest**（5 个测试用例 - 导航流程）✨ 新增
- [x] **PhotoDataFlowIntegrationTest**（6 个测试用例 - 数据流）✨ 新增

**总计：50 个测试用例**
- 单元测试：17 个
- 集成测试：33 个

### 🚧 待添加的测试

- [ ] PhotoAdapter 单元测试
- [ ] PhotoGroupAdapter 单元测试
- [ ] PhotoDetailAdapter UI 测试
- [ ] 性能测试
- [ ] 内存泄漏测试

---

*最后更新：2025-01-07*