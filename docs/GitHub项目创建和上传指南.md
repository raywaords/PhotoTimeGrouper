# GitHub 项目创建和上传指南

## 📋 概述

本文档详细说明如何在 GitHub 上创建新项目，以及如何将 PhotoTimeGrouper 代码上传到 GitHub。

**文档版本**：1.0  
**最后更新**：2024年

---

## 一、在 GitHub 上创建新仓库

### 1.1 登录 GitHub

1. 访问 [GitHub.com](https://github.com)
2. 登录您的账户（如果没有账户，先注册）

### 1.2 创建新仓库

1. 点击右上角的 **"+"** 按钮
2. 选择 **"New repository"**（新建仓库）

### 1.3 填写仓库信息

**基本信息**：
- **Repository name**（仓库名称）：例如 `PhotoTimeGrouper`
- **Description**（描述）：例如 "一个 Android 相册应用，按日期分组显示照片"
- **Visibility**（可见性）：
  - ✅ **Public**（公开）：任何人都可以看到
  - 🔒 **Private**（私有）：只有您和您授权的人可以看到

**初始化选项**：
- ❌ **不要勾选** "Add a README file"（我们已经有 README）
- ❌ **不要勾选** "Add .gitignore"（我们已经配置了）
- ❌ **不要勾选** "Choose a license"（可选，稍后可以添加）

### 1.4 创建仓库

点击 **"Create repository"** 按钮创建仓库。

**重要**：创建后，GitHub 会显示一个页面，**不要按照页面上的说明操作**（因为我们已经有本地代码了），直接关闭即可。

### 1.5 创建仓库时常见错误及解决方案

如果在点击 "Create repository" 时遇到错误，请参考以下解决方案：

#### ❌ 错误 1: "Name already exists on this account"
**原因**：仓库名称已被使用（您之前可能已经创建过同名仓库）

**解决方案**：
1. 更改仓库名称，例如：
   - `PhotoTimeGrouper` → `PhotoTimeGrouper-v2`
   - `PhotoTimeGrouper` → `PhotoTimeGrouper-Android`
   - `PhotoTimeGrouper` → `MyPhotoTimeGrouper`
2. 或者删除之前的同名仓库（如果不再需要）

#### ❌ 错误 2: "Name contains invalid characters"
**原因**：仓库名称包含不允许的字符

**解决方案**：
- ✅ **允许的字符**：字母、数字、连字符（`-`）、下划线（`_`）
- ❌ **不允许的字符**：空格、特殊符号（如 `@`, `#`, `$`, `%` 等）
- ✅ **正确示例**：`PhotoTimeGrouper`, `photo-time-grouper`, `photo_time_grouper`
- ❌ **错误示例**：`Photo Time Grouper`（包含空格）

#### ❌ 错误 3: "Name is too long" 或 "Name must be less than 100 characters"
**原因**：仓库名称太长

**解决方案**：
- 缩短仓库名称，建议不超过 50 个字符
- 例如：`PhotoTimeGrouper` 而不是 `MyAwesomePhotoTimeGrouperApplicationForAndroid`

#### ❌ 错误 4: "Something went wrong" 或网络错误
**原因**：网络连接问题或 GitHub 服务器问题

**解决方案**：
1. **检查网络连接**：
   - 确认能正常访问 GitHub.com
   - 尝试刷新页面（F5）
   - 清除浏览器缓存

2. **尝试其他浏览器**：
   - 如果使用 Chrome，尝试 Edge 或 Firefox
   - 禁用浏览器扩展（特别是广告拦截器）

3. **检查账户状态**：
   - 确认 GitHub 账户已验证邮箱
   - 检查是否有未读的 GitHub 通知

4. **稍后重试**：
   - 等待几分钟后重试
   - GitHub 偶尔会有临时服务中断

#### ❌ 错误 5: "Repository name cannot start with a dot"
**原因**：仓库名称以点（`.`）开头

**解决方案**：
- 不要以点开头，例如：`.PhotoTimeGrouper` ❌
- 改为：`PhotoTimeGrouper` ✅

#### ❌ 错误 6: "Repository name cannot end with .git"
**原因**：仓库名称以 `.git` 结尾

**解决方案**：
- GitHub 会自动添加 `.git`，您不需要手动添加
- 例如：`PhotoTimeGrouper.git` ❌ → `PhotoTimeGrouper` ✅

#### ❌ 错误 7: 账户限制（免费账户）
**原因**：免费账户可能有一些限制

**解决方案**：
1. **检查账户类型**：
   - 免费账户可以创建无限公开仓库
   - 免费账户可以创建有限数量的私有仓库
   - 如果达到私有仓库限制，可以：
     - 删除不需要的私有仓库
     - 将一些私有仓库改为公开
     - 升级到付费账户

2. **验证邮箱**：
   - 确保 GitHub 账户已验证邮箱
   - 未验证邮箱可能导致某些功能受限

#### 🔧 通用排查步骤

如果遇到其他错误，按以下步骤排查：

1. **检查仓库名称**：
   - ✅ 只包含字母、数字、连字符、下划线
   - ✅ 长度在 1-100 个字符之间
   - ✅ 不以点或 `.git` 结尾

2. **检查浏览器**：
   - 清除缓存和 Cookie
   - 尝试无痕/隐私模式
   - 禁用浏览器扩展

3. **检查网络**：
   - 尝试使用 VPN（如果在某些地区）
   - 检查防火墙设置
   - 尝试使用移动网络

4. **联系 GitHub 支持**：
   - 如果以上方法都不行，访问 [GitHub Support](https://support.github.com)
   - 提供错误截图和详细描述

#### ✅ 推荐的仓库名称

以下是一些推荐的仓库名称格式：

- `PhotoTimeGrouper` ✅
- `photo-time-grouper` ✅
- `PhotoTimeGrouper-Android` ✅
- `phototimegrouper-app` ✅

**避免使用**：
- `Photo Time Grouper` ❌（包含空格）
- `PhotoTimeGrouper.git` ❌（不需要 .git）
- `.PhotoTimeGrouper` ❌（以点开头）

---

## 二、准备本地代码

### 2.1 检查 .gitignore 文件

确认 `.gitignore` 文件已正确配置，排除敏感文件：

```bash
# 检查 .gitignore 内容
cat .gitignore
```

**应该排除的文件**：
- ✅ `*.keystore` - 密钥库文件
- ✅ `keystore.properties` - 密钥库配置（包含密码）
- ✅ `build/` - 构建输出
- ✅ `.idea/` - Android Studio 配置
- ✅ `*.iml` - IntelliJ 项目文件

### 2.2 检查敏感文件

**确保以下文件不会被提交**：
- ❌ `keystore.properties`（如果存在）
- ❌ `phototimegrouper.keystore`（如果存在）
- ❌ 任何包含密码或密钥的文件

**检查命令**：
```bash
# Windows PowerShell
Get-ChildItem -Recurse -Include "*.keystore","keystore.properties" | Select-Object FullName

# 如果发现这些文件，确认它们已在 .gitignore 中
```

---

## 三、初始化 Git 仓库（如果还没有）

### 3.1 检查是否已有 Git 仓库

```bash
# 检查是否已有 .git 目录
ls -la .git
# 或 Windows
Test-Path .git
```

### 3.2 如果没有，初始化 Git 仓库

```bash
# 在项目根目录执行
git init
```

---

## 四、配置 Git（首次使用）

### 4.1 设置用户名和邮箱

```bash
# 设置全局用户名（替换为您的 GitHub 用户名）
git config --global user.name "YourGitHubUsername"

# 设置全局邮箱（替换为您的 GitHub 邮箱）
git config --global user.email "your.email@example.com"
```

**或者仅为当前项目设置**：
```bash
# 仅在当前项目设置
git config user.name "YourGitHubUsername"
git config user.email "your.email@example.com"
```

### 4.2 验证配置

```bash
git config --list
```

---

## 五、添加文件到 Git

### 5.1 查看当前状态

```bash
git status
```

### 5.2 添加所有文件

```bash
# 添加所有文件（.gitignore 会自动排除不需要的文件）
git add .
```

### 5.3 查看将要提交的文件

```bash
# 确认没有敏感文件被添加
git status
```

**重要检查**：
- ✅ 确认 `keystore.properties` **没有**出现在列表中
- ✅ 确认 `*.keystore` 文件**没有**出现在列表中
- ✅ 确认 `build/` 目录**没有**出现在列表中

### 5.4 提交文件

```bash
# 提交文件，添加提交信息
git commit -m "Initial commit: PhotoTimeGrouper Android app"
```

---

## 六、连接到 GitHub 仓库

### 6.1 获取 GitHub 仓库地址

在 GitHub 仓库页面，点击绿色的 **"Code"** 按钮，复制仓库地址：

- **HTTPS 方式**：`https://github.com/YourUsername/PhotoTimeGrouper.git`
- **SSH 方式**：`git@github.com:YourUsername/PhotoTimeGrouper.git`

**推荐使用 HTTPS**（更简单，不需要配置 SSH 密钥）

### 6.2 添加远程仓库

```bash
# 添加远程仓库（替换为您的实际仓库地址）
git remote add origin https://github.com/YourUsername/PhotoTimeGrouper.git

# 验证远程仓库
git remote -v
```

### 6.3 重命名主分支（如果需要）

GitHub 现在默认使用 `main` 分支，如果您的本地分支是 `master`：

```bash
# 重命名分支
git branch -M main
```

---

## 七、上传代码到 GitHub

### 7.1 推送代码

```bash
# 推送代码到 GitHub（首次推送）
git push -u origin main
```

**如果遇到错误**：
- 如果提示需要认证，GitHub 现在要求使用 Personal Access Token（个人访问令牌）
- 参见下面的"认证配置"部分

### 7.2 验证上传

1. 刷新 GitHub 仓库页面
2. 确认所有文件都已上传
3. 确认 README.md 正确显示

---

## 八、认证配置（GitHub 要求）

### 8.1 创建 Personal Access Token

GitHub 不再支持密码认证，需要使用 Personal Access Token：

1. 登录 GitHub
2. 点击右上角头像 → **Settings**
3. 左侧菜单选择 **Developer settings**
4. 选择 **Personal access tokens** → **Tokens (classic)**
5. 点击 **Generate new token** → **Generate new token (classic)**
6. 填写信息：
   - **Note**：例如 "PhotoTimeGrouper Local Development"
   - **Expiration**：选择过期时间（建议 90 天或更长）
   - **Scopes**：勾选 `repo`（完整仓库访问权限）
7. 点击 **Generate token**
8. **重要**：复制生成的 token（只显示一次，请妥善保存）

### 8.2 使用 Token 推送

当 Git 提示输入密码时：
- **Username**：输入您的 GitHub 用户名
- **Password**：输入刚才创建的 Personal Access Token（不是 GitHub 密码）

### 8.3 保存凭据（可选）

**Windows**：
```bash
# 使用 Git Credential Manager（通常已安装）
git config --global credential.helper manager-core
```

**Mac/Linux**：
```bash
# 使用 credential helper
git config --global credential.helper store
```

---

## 九、后续更新代码

### 9.1 日常更新流程

```bash
# 1. 查看更改
git status

# 2. 添加更改的文件
git add .

# 3. 提交更改
git commit -m "描述您的更改"

# 4. 推送到 GitHub
git push
```

### 9.2 提交信息规范

**好的提交信息示例**：
```bash
git commit -m "添加隐私政策链接功能"
git commit -m "修复视频播放时的显示问题"
git commit -m "更新 README 文档"
```

**避免的提交信息**：
```bash
git commit -m "更新"  # 太模糊
git commit -m "fix"    # 不具体
```

---

## 十、常见问题

### Q1: 推送时提示 "remote: Support for password authentication was removed"
**A**: GitHub 不再支持密码认证，需要使用 Personal Access Token。参见"认证配置"部分。

### Q2: 如何避免提交敏感文件？
**A**: 
- ✅ 确保 `.gitignore` 文件正确配置
- ✅ 使用 `git status` 检查将要提交的文件
- ✅ 如果已经提交了敏感文件，需要从 Git 历史中删除（参见下面的"紧急处理"）

### Q3: 如果误提交了敏感文件怎么办？
**A**: **紧急处理步骤**：

```bash
# 1. 立即从 Git 中删除文件（但保留本地文件）
git rm --cached keystore.properties
git rm --cached *.keystore

# 2. 提交删除操作
git commit -m "Remove sensitive files"

# 3. 推送到 GitHub
git push

# 4. 重要：如果文件已经推送到 GitHub，需要：
#    - 立即在 GitHub 上删除仓库
#    - 或使用 git-filter-repo 工具清理历史
#    - 重新创建仓库并推送
```

### Q4: 如何查看提交历史？
**A**: 
```bash
git log
git log --oneline  # 简洁版本
```

### Q5: 如何撤销最后一次提交？
**A**: 
```bash
# 撤销提交但保留更改
git reset --soft HEAD~1

# 完全撤销提交和更改（谨慎使用）
git reset --hard HEAD~1
```

### Q6: 如何创建分支？
**A**: 
```bash
# 创建新分支
git checkout -b feature/new-feature

# 切换到分支
git checkout feature/new-feature

# 查看所有分支
git branch
```

---

## 十一、GitHub 仓库设置建议

### 11.1 添加仓库描述和主题

1. 在仓库页面点击 **Settings**
2. 在 **General** 部分：
   - 更新 **Description**
   - 添加 **Topics**（标签）：例如 `android`, `kotlin`, `photo-gallery`, `media-manager`

### 11.2 添加 README 徽章（可选）

在 README.md 中添加一些徽章，例如：
```markdown
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=flat&logo=kotlin&logoColor=white)
```

### 11.3 添加 LICENSE（可选）

1. 在仓库页面点击 **Add file** → **Create new file**
2. 文件名输入 `LICENSE`
3. 选择许可证类型（如 MIT、Apache 2.0 等）
4. 提交文件

---

## 十二、安全检查清单

在上传代码前，请确认：

- [ ] `.gitignore` 文件已正确配置
- [ ] `keystore.properties` 文件**没有**被提交
- [ ] `*.keystore` 文件**没有**被提交
- [ ] 没有硬编码的密码或 API 密钥
- [ ] 没有个人信息（如真实邮箱、电话）在代码中
- [ ] README.md 中的信息是公开可分享的

---

## 十三、快速命令参考

```bash
# 初始化仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "提交信息"

# 添加远程仓库
git remote add origin https://github.com/YourUsername/PhotoTimeGrouper.git

# 推送代码
git push -u origin main

# 查看状态
git status

# 查看提交历史
git log --oneline

# 拉取最新代码
git pull
```

---

## 十四、总结

### 完整流程

1. ✅ 在 GitHub 上创建新仓库
2. ✅ 在本地初始化 Git（如果还没有）
3. ✅ 配置 Git 用户名和邮箱
4. ✅ 添加文件到 Git
5. ✅ 提交文件
6. ✅ 连接 GitHub 远程仓库
7. ✅ 创建 Personal Access Token
8. ✅ 推送代码到 GitHub

### 重要提醒

- ⚠️ **永远不要提交密钥库文件和密码**
- ⚠️ **使用 Personal Access Token 而不是密码**
- ✅ **定期提交和推送代码**
- ✅ **使用有意义的提交信息**

---

**文档版本**：1.0  
**最后更新**：2024年

**祝您使用愉快！** 🎉
