# 快速下载指南

## 最简单的方法

### 1. 下载 Gradle 8.5 完整版

**推荐使用阿里云镜像**（国内访问快）：
```
https://mirrors.aliyun.com/gradle/gradle-8.5-bin.zip
```

或官方源：
```
https://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### 2. 解压并复制文件

1. 解压下载的 `gradle-8.5-bin.zip`
2. 找到文件：`gradle-8.5\gradle\wrapper\gradle-wrapper.jar`
3. 复制这个文件
4. 粘贴到：`PhotoTimeGrouper\gradle\wrapper\gradle-wrapper.jar`

### 3. 验证

运行：
```cmd
cd C:\Users\leiyu\Desktop\PhotoTimeGrouper
gradlew --version
```

如果显示 Gradle 版本信息，说明修复成功！

## 文件位置

**源文件位置**（解压后）：
```
gradle-8.5/
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.properties
        └── gradle-wrapper.jar  ← 复制这个文件
```

**目标位置**（项目目录）：
```
PhotoTimeGrouper/
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.properties  (已有)
        └── gradle-wrapper.jar  ← 粘贴到这里
```

## 如果下载很慢

可以尝试：
1. 使用下载工具（如 IDM、迅雷等）加速
2. 使用手机热点下载
3. 请朋友帮忙下载后传输

## 验证文件

下载并复制后，检查文件：
```cmd
dir gradle\wrapper\gradle-wrapper.jar
```

应该显示文件大小约 **60-70 KB**。
