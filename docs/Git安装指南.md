# Git 安装指南（Windows）

## 📋 概述

在将代码上传到 GitHub 之前，需要先在 Windows 系统上安装 Git。

---

## 一、下载 Git

### 方法 1：官方下载（推荐）

1. 访问 Git 官方网站：https://git-scm.com/download/win
2. 页面会自动检测您的系统并开始下载
3. 下载的文件名类似：`Git-2.xx.x-64-bit.exe`

### 方法 2：使用包管理器

**使用 Chocolatey**（如果已安装）：
```powershell
choco install git
```

**使用 winget**（Windows 10/11）：
```powershell
winget install --id Git.Git -e --source winget
```

---

## 二、安装 Git

### 2.1 运行安装程序

1. 双击下载的 `.exe` 文件
2. 如果出现用户账户控制（UAC）提示，点击 **"是"**

### 2.2 安装选项配置

#### 步骤 1：许可协议
- 阅读许可协议
- 点击 **"Next"**

#### 步骤 2：选择安装位置
- 默认位置通常是：`C:\Program Files\Git`
- 可以保持默认，或选择其他位置
- 点击 **"Next"**

#### 步骤 3：选择组件（Components）
**推荐配置**：
- ✅ **Git Bash Here** - 在右键菜单添加 Git Bash
- ✅ **Git GUI Here** - 在右键菜单添加 Git GUI
- ✅ **Associate .git* configuration files with the default text editor** - 关联配置文件
- ✅ **Associate .sh files to be run with Bash** - 关联 shell 脚本
- ✅ **Check daily for Git for Windows updates** - 每日检查更新

点击 **"Next"**

#### 步骤 4：选择默认编辑器（Default Editor）
- 推荐选择：**"Use Visual Studio Code as Git's default editor"**（如果已安装 VS Code）
- 或选择：**"Nano editor"**（简单易用）
- 或选择：**"Notepad++"**（如果已安装）

点击 **"Next"**

#### 步骤 5：调整 PATH 环境变量（Adjusting your PATH environment）
**重要**：选择 **"Git from the command line and also from 3rd-party software"**

这个选项会：
- ✅ 将 Git 添加到系统 PATH
- ✅ 可以在 PowerShell、CMD 等任何地方使用 `git` 命令
- ✅ 这是**推荐选项**

点击 **"Next"**

#### 步骤 6：选择 HTTPS 传输后端（Choosing HTTPS transport backend）
**推荐**：选择 **"Use the OpenSSL library"**

点击 **"Next"**

#### 步骤 7：配置行尾转换（Configuring the line ending conversions）
**推荐**：选择 **"Checkout Windows-style, commit Unix-style line endings"**

这个选项：
- ✅ 在 Windows 上使用 CRLF（Windows 风格）
- ✅ 提交到 Git 时自动转换为 LF（Unix 风格）
- ✅ 适合大多数情况

点击 **"Next"**

#### 步骤 8：配置终端模拟器（Configuring the terminal emulator）
**推荐**：选择 **"Use Windows' default console window"**

点击 **"Next"**

#### 步骤 9：配置额外选项（Configuring extra options）
**推荐配置**：
- ✅ **Enable file system caching** - 启用文件系统缓存（提升性能）
- ✅ **Enable Git Credential Manager** - 启用凭据管理器（方便认证）

点击 **"Next"**

#### 步骤 10：配置实验性选项（Configuring experimental options）
- ❌ 通常不需要勾选实验性选项
- 直接点击 **"Install"**

### 2.3 完成安装

1. 等待安装完成（通常需要 1-2 分钟）
2. 安装完成后，点击 **"Finish"**
3. **重要**：如果提示重启，建议重启计算机（虽然通常不需要）

---

## 三、验证安装

### 3.1 打开新的 PowerShell 窗口

**重要**：安装 Git 后，需要**关闭并重新打开** PowerShell 或命令提示符，环境变量才会生效。

### 3.2 检查 Git 版本

```powershell
git --version
```

**预期输出**：
```
git version 2.xx.x.windows.x
```

如果显示版本号，说明安装成功！✅

### 3.3 检查 Git 配置

```powershell
git config --list
```

---

## 四、配置 Git（首次使用）

### 4.1 设置用户名

```powershell
git config --global user.name "您的GitHub用户名"
```

**示例**：
```powershell
git config --global user.name "leiyu"
```

### 4.2 设置邮箱

```powershell
git config --global user.email "您的GitHub邮箱"
```

**示例**：
```powershell
git config --global user.email "your.email@example.com"
```

**重要**：邮箱应该与您的 GitHub 账户邮箱一致（或已添加到 GitHub 账户的邮箱列表中）。

### 4.3 验证配置

```powershell
git config --global --list
```

应该看到：
```
user.name=您的用户名
user.email=您的邮箱
```

---

## 五、常见问题

### Q1: 安装后仍然提示 "git 不是内部或外部命令"
**A**: 
1. **关闭并重新打开** PowerShell/CMD 窗口
2. 如果还是不行，检查环境变量：
   - 打开"系统属性" → "高级" → "环境变量"
   - 在"系统变量"中找到 `Path`
   - 确认包含：`C:\Program Files\Git\cmd`
3. 如果还是没有，可能需要重启计算机

### Q2: 如何更新 Git？
**A**: 
```powershell
# 使用 Git 自带的更新命令
git update-git-for-windows
```

或重新下载最新版本安装程序。

### Q3: 安装时选择了错误的选项怎么办？
**A**: 可以重新运行安装程序，选择"修改"（Modify）来更改配置。

---

## 六、安装完成后的下一步

安装并配置好 Git 后，您可以：

1. ✅ 返回 [GitHub项目创建和上传指南.md](./GitHub项目创建和上传指南.md)
2. ✅ 继续执行"初始化 Git 仓库"步骤
3. ✅ 将代码上传到 GitHub

---

## 七、快速安装命令（如果使用 winget）

如果您使用 Windows 10/11 的 `winget` 包管理器，可以快速安装：

```powershell
# 安装 Git
winget install --id Git.Git -e --source winget

# 安装后，关闭并重新打开 PowerShell
# 然后配置 Git
git config --global user.name "您的GitHub用户名"
git config --global user.email "您的GitHub邮箱"
```

---

**文档版本**：1.0  
**最后更新**：2024年

**安装完成后，请继续执行 GitHub 上传指南的后续步骤！** 🚀
