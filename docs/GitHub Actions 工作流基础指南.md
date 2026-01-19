# GitHub Actions 工作流基础指南

## 📋 文档概述

本文档全面介绍 GitHub Actions 工作流的基础知识、工作原理、实施步骤，以及在本项目中的实际应用。

**适用对象**：想要了解 CI/CD 工作流的开发者  
**项目**：PhotoTimeGrouper Android 应用  
**CI/CD 平台**：GitHub Actions

---

## 一、GitHub Actions 基础概念

### 1.1 什么是 GitHub Actions？

GitHub Actions 是 GitHub 提供的持续集成和持续部署（CI/CD）平台，允许你直接在 GitHub 仓库中自动化工作流程。

**核心概念：**
- **工作流（Workflow）**：自动化的流程，由一个或多个作业组成
- **作业（Job）**：在同一运行器上执行的一组步骤
- **步骤（Step）**：单个任务，可以运行命令或使用操作
- **操作（Action）**：可重用的代码单元，封装了常见的任务

### 1.2 工作原理

```
代码 Push/Pull Request
    ↓
触发工作流（Trigger）
    ↓
GitHub Actions 运行器（Runner）
    ↓
执行作业（Jobs）
    ↓
运行步骤（Steps）
    ↓
生成结果/报告
```

### 1.3 工作流文件位置

工作流文件必须放在 `.github/workflows/` 目录下，文件格式为 YAML（`.yml` 或 `.yaml`）。

**示例结构：**
```
项目根目录/
└── .github/
    └── workflows/
        ├── build-and-test.yml
        └── test.yml
```

---

## 二、工作流文件基本结构

### 2.1 最小工作流示例

```yaml
name: 工作流名称

on:
  push:
    branches: [ main ]

jobs:
  作业名称:
    runs-on: ubuntu-latest
    
    steps:
    - name: 步骤名称
      run: echo "Hello, World!"
```

### 2.2 关键组成部分

#### 2.2.1 `name` - 工作流名称

```yaml
name: 构建和测试
```
- 显示在 GitHub Actions 页面的工作流名称
- 可以省略，默认为文件路径

#### 2.2.2 `on` - 触发条件

**Push 触发：**
```yaml
on:
  push:
    branches: [ main ]
```

**Pull Request 触发：**
```yaml
on:
  pull_request:
    branches: [ main ]
```

**多种触发方式：**
```yaml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 0 * * *'  # 每天午夜运行
  workflow_dispatch:  # 手动触发
```

#### 2.2.3 `jobs` - 作业定义

```yaml
jobs:
  作业ID:
    runs-on: ubuntu-latest  # 运行环境
    permissions:            # 权限配置（可选）
      checks: write
      contents: read
    steps:                 # 步骤列表
      - name: 步骤1
        run: 命令
```

**常见的运行器（Runner）：**
- `ubuntu-latest` - Ubuntu Linux（最常用）
- `windows-latest` - Windows
- `macos-latest` - macOS

#### 2.2.4 `steps` - 步骤定义

**使用 Action（推荐）：**
```yaml
- name: 检出代码
  uses: actions/checkout@v4
```

**运行命令：**
```yaml
- name: 构建项目
  run: ./gradlew build
```

**运行多行命令：**
```yaml
- name: 安装依赖
  run: |
    echo "开始安装..."
    npm install
    echo "安装完成"
```

---

## 三、本项目中的工作流

目前项目中有 **2 个工作流文件**：

### 3.1 工作流 1：构建和测试（`build-and-test.yml`）

**功能**：完整的构建和测试流程

**触发条件：**
- Push 到 `main` 分支
- Pull Request 到 `main` 分支

**主要步骤：**
1. 检出代码
2. 设置 JDK 17
3. 设置 Android SDK
4. 授予 gradlew 执行权限
5. 检查 gradle-wrapper.jar
6. 构建 Debug APK
7. 运行单元测试
8. 生成测试覆盖率报告
9. 上传构建产物（APK）
10. 上传测试报告
11. 上传覆盖率报告
12. 显示测试摘要

**特点：**
- ✅ 最完整的工作流
- ✅ 生成所有类型的报告
- ✅ 适用于正式发布前验证

---

### 3.2 工作流 2：单元测试（`test.yml`）

**功能**：快速单元测试（轻量级）

**触发条件：**
- Push 到 `main` 或 `develop` 分支
- Pull Request 到 `main` 或 `develop` 分支

**主要步骤：**
1. 检出代码
2. 设置 JDK 17
3. 授予 gradlew 执行权限
4. 运行单元测试
5. 生成测试报告
6. 上传测试结果（创建 Check Run）

**特点：**
- ✅ 快速执行（不构建 APK）
- ✅ 在 PR 中显示测试结果
- ✅ 适用于日常开发快速验证

---

## 四、工作流执行流程详解

### 4.1 完整执行流程

以 `build-and-test.yml` 为例：

```
1. 代码 Push/Pull Request
   ↓
2. GitHub Actions 检测到触发条件
   ↓
3. 创建运行器环境（Ubuntu）
   ↓
4. 检出代码（actions/checkout@v4）
   ↓
5. 设置环境（JDK、Android SDK）
   ↓
6. 执行构建步骤
   - 检查文件
   - 构建 APK
   ↓
7. 执行测试步骤
   - 运行测试
   - 生成报告
   ↓
8. 上传结果
   - 上传 APK
   - 上传报告
   ↓
9. 生成摘要
   ↓
10. 完成（显示成功/失败状态）
```

### 4.2 关键步骤说明

#### 步骤 1：检出代码

```yaml
- name: 检出代码
  uses: actions/checkout@v4
```

**作用**：将仓库代码下载到运行器的工作目录  
**必须**：几乎每个工作流的第一步  
**原因**：运行器是全新的环境，没有你的代码

#### 步骤 2：设置 Java 环境

```yaml
- name: 设置 JDK 17
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '17'
    cache: 'gradle'
```

**作用**：
- 安装指定版本的 Java
- 配置 JAVA_HOME 环境变量
- 缓存 Gradle 依赖（`cache: 'gradle'`）加速后续构建

**为什么需要**：Android 项目需要 Java 来编译 Kotlin/Java 代码

#### 步骤 3：设置 Android SDK

```yaml
- name: 设置 Android SDK
  uses: android-actions/setup-android@v3
```

**作用**：安装和配置 Android SDK  
**为什么需要**：Android 项目需要 SDK 来编译和构建 APK

#### 步骤 4：构建 APK

```yaml
- name: 构建 Debug APK
  run: |
    ./gradlew assembleDebug --no-daemon --stacktrace 2>&1 | tee build.log
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
      echo "=== 编译错误详情 ==="
      grep -A 5 -B 5 "error\|Error\|Unresolved\|Expecting" build.log || cat build.log
      exit 1
    fi
```

**关键参数：**
- `--no-daemon`：不使用 Gradle 守护进程（CI 环境推荐）
- `--stacktrace`：显示详细错误堆栈
- `2>&1 | tee`：同时输出到控制台和文件
- 错误处理：提取关键错误信息

#### 步骤 5：运行测试

```yaml
- name: 运行单元测试
  run: ./gradlew testDebugUnitTest --no-daemon
  continue-on-error: true
```

**关键点：**
- `testDebugUnitTest`：Android Gradle Plugin 8.0+ 的测试任务名称
- `continue-on-error: true`：即使测试失败也继续后续步骤（上传报告）

#### 步骤 6：上传产物

```yaml
- name: 上传构建产物
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: debug-apk
    path: app/build/outputs/apk/debug/*.apk
    retention-days: 7
```

**关键点：**
- `if: always()`：无论前面步骤成功或失败都执行
- `name`：Artifact 名称（下载时显示）
- `path`：要上传的文件路径
- `retention-days`：保留天数（GitHub 免费版限制 90 天）

---

## 五、其他常见工作流类型

除了测试工作流，还可以创建以下类型的工作流：

### 5.1 代码质量检查工作流

**用途**：检查代码风格、静态分析

**示例功能：**
- Lint 检查
- 代码格式化验证
- 安全检查

**示例结构：**
```yaml
name: 代码质量检查

on:
  pull_request:
    branches: [ main ]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: 运行 Lint
      run: ./gradlew lint
```

### 5.2 发布构建工作流

**用途**：构建发布版本的 APK/AAB

**示例功能：**
- 构建 Release APK
- 构建 Android App Bundle (AAB)
- 自动版本号管理
- 签名 APK

**关键点：**
- 需要使用 GitHub Secrets 存储签名密钥
- 通常只在打 Tag 或手动触发时运行

### 5.3 部署工作流

**用途**：自动部署到应用商店或测试平台

**示例功能：**
- 发布到 Google Play（内测/正式）
- 发布到 Firebase App Distribution
- 发布到内部测试服务器

### 5.4 依赖更新工作流

**用途**：自动检查并更新依赖

**示例功能：**
- Dependabot 自动更新
- 依赖安全检查
- 版本冲突检查

### 5.5 文档生成工作流

**用途**：自动生成和发布文档

**示例功能：**
- 生成 API 文档
- 发布到 GitHub Pages
- 更新 README 统计信息

---

## 六、工作流实施步骤

### 6.1 创建新工作流的步骤

**步骤 1：创建文件**
```
在 .github/workflows/ 目录下创建 .yml 文件
```

**步骤 2：定义基本结构**
```yaml
name: 工作流名称

on:
  # 定义触发条件

jobs:
  作业名称:
    runs-on: ubuntu-latest
    steps:
      # 定义步骤
```

**步骤 3：添加步骤**
- 先添加"检出代码"
- 然后添加环境设置
- 最后添加业务逻辑

**步骤 4：测试工作流**
- Push 到分支触发
- 查看 Actions 页面
- 检查日志输出

**步骤 5：优化和调整**
- 添加错误处理
- 优化执行速度
- 添加缓存

---

## 七、常用 Actions 和技巧

### 7.1 常用 Actions

| Action | 用途 | 示例 |
|--------|------|------|
| `actions/checkout@v4` | 检出代码 | 第一步必需 |
| `actions/setup-java@v4` | 设置 Java 环境 | Android 项目必需 |
| `android-actions/setup-android@v3` | 设置 Android SDK | Android 项目必需 |
| `actions/upload-artifact@v4` | 上传文件 | 保存构建产物 |
| `actions/download-artifact@v4` | 下载文件 | 跨作业共享文件 |
| `dorny/test-reporter@v1` | 测试报告 | 在 PR 中显示测试结果 |

### 7.2 性能优化技巧

**1. 使用缓存**
```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'gradle'  # 缓存 Gradle 依赖
```

**2. 并行执行作业**
```yaml
jobs:
  build:
    # 构建作业
  
  test:
    # 测试作业（与 build 并行）
```

**3. 条件执行**
```yaml
- name: 部署
  if: github.ref == 'refs/heads/main'  # 只在 main 分支执行
  run: deploy.sh
```

---

## 八、查看和管理工作流

### 8.1 在 GitHub 上查看

**查看工作流运行：**
1. 访问仓库页面
2. 点击 "Actions" 标签
3. 选择工作流查看运行历史

**查看单个运行：**
- 点击运行记录查看详细信息
- 查看每个步骤的日志
- 下载 Artifacts

### 8.2 手动触发工作流

**方法 1：使用 `workflow_dispatch`**
```yaml
on:
  workflow_dispatch:  # 允许手动触发
```

在 Actions 页面点击 "Run workflow" 按钮

**方法 2：重新运行失败的运行**
- 在运行页面点击 "Re-run jobs"

---

## 九、故障排查

### 9.1 常见问题

**问题 1：工作流没有触发**
- 检查触发条件是否正确
- 确认分支名称匹配
- 检查文件语法错误

**问题 2：步骤失败**
- 查看日志中的错误信息
- 检查环境配置
- 验证文件路径

**问题 3：权限错误**
- 添加 `permissions` 配置
- 检查 GitHub Secrets 配置

---

## 十、最佳实践

### 10.1 工作流设计

1. **单一职责**：每个工作流专注于一个任务
2. **快速反馈**：优先快速验证，再完整构建
3. **错误处理**：使用 `continue-on-error` 和 `if: always()`
4. **清晰命名**：使用描述性的名称和步骤名称

### 10.2 安全实践

1. **不要提交密钥**：使用 GitHub Secrets
2. **最小权限**：只授予必需的权限
3. **敏感信息**：不要在日志中输出密码

### 10.3 性能优化

1. **使用缓存**：缓存依赖和构建产物
2. **并行执行**：独立的作业并行运行
3. **条件执行**：只在需要时执行昂贵操作

---

## 十一、参考资源

### 11.1 官方文档

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [工作流语法](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [Android Actions](https://github.com/android-actions/setup-android)

### 11.2 项目文档

- `docs/工作流搭建与问题排查.md` - 本项目工作流问题排查
- `.github/workflows/build-and-test.yml` - 构建测试工作流
- `.github/workflows/test.yml` - 单元测试工作流

---

**文档版本**：1.0  
**最后更新**：2025年1月  
**维护者**：项目开发团队
