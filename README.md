# Photo Time Grouper

一个 Android 应用程序，按拍摄日期对设备相册中的照片进行分组显示。

## 功能特性

- 自动扫描设备相册
- 按拍摄日期对照片进行分组
- 以清晰、有序的界面显示照片
- 使用 Kotlin 和现代 Android 开发实践
- 使用协程进行异步加载，提升性能
- 线程安全的日期格式化工具

## 未来计划

查看详细的功能优化建议和改造方案，请参考：[ENHANCEMENTS.md](ENHANCEMENTS.md)

主要包括：
- 🔥 照片详情查看功能（高优先级）
- 🔥 下拉刷新功能
- 🔥 空状态提示
- 🎯 搜索功能
- 🎯 UI/UX 优化
- ⚡ 性能优化
- ⚡ 照片统计信息
- 💡 分享功能

## 技术细节

- **语言**: Kotlin
- **架构**: 嵌套 RecyclerView 适配器实现分组显示
- **异步处理**: Kotlin 协程 (Coroutines)
- **权限**: READ_MEDIA_IMAGES (Android 13+) 或 READ_EXTERNAL_STORAGE (旧版本)
- **图片加载**: Glide 库，支持缓存和错误处理
- **UI 组件**: RecyclerView, CardView, ConstraintLayout, ViewBinding

## 项目结构

- `MainActivity.kt`: 主活动，处理权限请求和照片加载
- `PhotoItem.kt`: 照片信息数据类
- `PhotoAdapter.kt`: 单个照片的适配器（内层横向 RecyclerView）
- `PhotoGroupAdapter.kt`: 日期分组的适配器（外层垂直 RecyclerView）
- `DateFormatter.kt`: 线程安全的日期格式化工具类
- 布局文件：UI 组件布局

## 代码逻辑详解

### 整体架构

这是一个采用嵌套 RecyclerView 结构的照片分组应用：
- **外层 RecyclerView**: 按日期分组显示（`PhotoGroupAdapter`）
- **内层 RecyclerView**: 每个日期组内的照片横向滚动显示（`PhotoAdapter`）

### 1. MainActivity - 主活动

#### 初始化流程

应用启动时：
1. 使用 ViewBinding 初始化视图
2. 检查存储权限（根据 Android 版本选择不同权限）
3. 如果已有权限，直接加载照片；否则请求权限

#### 权限处理

- **Android 13+ (TIRAMISU)**: 使用 `READ_MEDIA_IMAGES` 权限
- **旧版本**: 使用 `READ_EXTERNAL_STORAGE` 权限

权限授予后，调用 `loadPhotos()` 开始加载照片。

#### 照片加载流程

使用 Kotlin 协程实现异步加载：

1. **显示进度条**: 提示用户正在加载
2. **IO 线程加载**: 在后台线程查询 MediaStore
3. **数据分组**: 按修改日期进行分组
4. **主线程更新 UI**: 切换到主线程设置 RecyclerView 适配器
5. **错误处理**: 捕获异常并显示错误提示

#### 从 MediaStore 读取照片

- 查询 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
- 获取字段：ID、显示名称、添加时间、修改时间
- 按添加时间降序排序
- 遍历 Cursor，创建 `PhotoItem` 对象列表
- 异常处理：跳过无法读取的照片项，避免崩溃

#### 按日期分组

使用 Kotlin 标准库的 `groupBy` 函数：
- 根据照片的修改日期（格式化为 "yyyy-MM-dd"）进行分组
- 返回 `Map<日期字符串, 照片列表>`

### 2. PhotoGroupAdapter - 日期分组适配器

**职责**: 管理外层垂直 RecyclerView，每个 item 代表一个日期组

**关键逻辑**:
- 日期列表按降序排序（最新的日期在前）
- ViewHolder 包含：
  - 日期标题 TextView
  - 横向 RecyclerView（用于显示该日期的照片）
- 绑定数据时：
  - 格式化日期标题（如 "January 15, 2024"）
  - 为每个日期组设置横向 RecyclerView 和 `PhotoAdapter`

### 3. PhotoAdapter - 照片适配器

**职责**: 管理内层横向 RecyclerView，显示单个日期组内的照片

**关键逻辑**:
- ViewHolder 包含图片视图和时间戳文本
- 使用 Glide 加载图片：
  - 占位图：加载中显示
  - 错误处理：加载失败显示占位图
  - 缓存策略：磁盘缓存所有图片
  - 图片裁剪：centerCrop 模式

### 4. DateFormatter - 日期格式化工具类

**线程安全设计**: 使用 `ThreadLocal` 为每个线程提供独立的 `SimpleDateFormat` 实例

**三种格式化方法**:
1. `formatDateForGroup()`: 将时间戳格式化为 "yyyy-MM-dd"（用于分组）
2. `formatDateTime()`: 将时间戳格式化为 "yyyy-MM-dd HH:mm:ss"（显示在照片下方）
3. `formatDateHeader()`: 将日期字符串格式化为易读格式（如 "January 15, 2024"）

### 5. PhotoItem - 数据模型

数据类，存储照片的完整信息：
- `id`: 照片在 MediaStore 中的 ID
- `uri`: 照片的 URI 地址
- `displayName`: 照片显示名称
- `dateAdded`: 添加时间（Unix 时间戳，秒）
- `dateModified`: 修改时间（Unix 时间戳，秒）

### 执行流程图

```
启动应用
    ↓
MainActivity.onCreate()
    ↓
检查权限
    ↓
[有权限] → loadPhotos()
    ↓
显示进度条
    ↓
协程启动 (IO线程)
    ↓
loadPhotosFromMediaStore()
    ├─ 查询 MediaStore
    ├─ 遍历 Cursor
    └─ 创建 PhotoItem 列表
    ↓
groupPhotosByDate()
    └─ 按日期分组
    ↓
切回主线程
    ↓
设置 PhotoGroupAdapter
    ↓
[外层 RecyclerView]
    ├─ 日期1 → PhotoGroupViewHolder
    │   └─ [内层横向 RecyclerView] → PhotoAdapter → 显示照片
    ├─ 日期2 → PhotoGroupViewHolder
    │   └─ [内层横向 RecyclerView] → PhotoAdapter → 显示照片
    └─ ...
```

### 关键设计亮点

1. **协程异步处理**: 使用 Kotlin 协程替代传统 Thread，代码更简洁，自动处理线程切换
2. **嵌套 RecyclerView**: 外层按日期分组，内层横向展示照片，实现清晰的层次结构
3. **线程安全**: `DateFormatter` 使用 `ThreadLocal` 确保多线程环境下的安全性
4. **错误处理**: 完善的异常捕获机制，避免应用崩溃
5. **性能优化**: 
   - Glide 图片缓存策略
   - 使用 Kotlin 标准库的 `groupBy` 函数
   - 日期列表预排序
   - ViewBinding 替代 findViewById

## 工作原理

1. 应用请求访问设备相册的权限
2. 查询 MediaStore 内容提供者以检索所有图片
3. 使用 `dateModified` 字段按日期对照片进行分组
4. UI 将每个日期组显示为一个部分，照片可以横向滚动查看

## 系统要求

- Android API 级别 24+（支持 API 33+ 的新权限模型）
- 存储访问权限

## 构建和运行

要构建并运行此项目：

1. 克隆或下载仓库
2. 在 Android Studio 中打开
3. 同步 Gradle 文件
4. 在物理设备或模拟器上构建并运行

**注意**: 应用需要访问照片的权限，首次启动时会请求该权限。

## 故障排除

如果在 Android Studio 中打开项目时遇到 "Read timed out" 错误，这通常是由于下载 Gradle 分发版时的网络连接问题导致的。解决方法：

1. 检查您的互联网连接
2. 如果使用 VPN 或代理，确保它允许连接到 gradle.org
3. 您也可以从 https://services.gradle.org/distributions/gradle-7.5-bin.zip 手动下载 Gradle 分发版，并将其放置在 Gradle 缓存目录中
4. 在 Android Studio 中，转到 File > Settings > Build > Build Tools > Gradle 并验证您的 Gradle 设置
