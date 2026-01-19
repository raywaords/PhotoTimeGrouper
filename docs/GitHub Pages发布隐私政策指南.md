# GitHub Pages 发布隐私政策指南

## 📋 概述

本指南详细说明如何使用 GitHub Pages 免费发布隐私政策网页，这是最简单且免费的发布方式。

**预计时间**：15-30 分钟  
**难度**：简单 ⭐

---

## 一、准备工作

### 1.1 确认文件已存在

确认以下文件已存在：
- ✅ `docs/隐私政策.html` - 隐私政策 HTML 文件

### 1.2 确认 GitHub 仓库

- ✅ 您的代码已上传到 GitHub
- ✅ 您有仓库的访问权限

---

## 二、方法 1：使用 docs 目录发布（推荐）⭐

这是最简单的方法，不需要创建新分支。

### 步骤 1：将 HTML 文件放到 docs 目录

**选项 A：如果您的仓库已经有 `docs` 目录**

1. 将 `docs/隐私政策.html` 文件**重命名**为 `privacy-policy.html`（使用英文文件名，避免中文路径问题）
2. 确保文件在 `docs/privacy-policy.html`

**选项 B：如果您的仓库没有 `docs` 目录**

1. 在项目根目录创建 `docs` 目录
2. 将 `docs/隐私政策.html` 复制到 `docs/privacy-policy.html`（使用英文文件名）

### 步骤 2：提交文件到 GitHub

如果您使用 Git 命令行：

```bash
# 1. 添加文件
git add docs/privacy-policy.html

# 2. 提交
git commit -m "Add privacy policy for GitHub Pages"

# 3. 推送到 GitHub
git push
```

**或者**直接在 GitHub 网站上：
1. 进入您的 GitHub 仓库
2. 点击 "Add file" → "Upload files"
3. 将 `privacy-policy.html` 拖拽到 `docs/` 目录
4. 填写提交信息："Add privacy policy for GitHub Pages"
5. 点击 "Commit changes"

### 步骤 3：启用 GitHub Pages

1. 进入您的 GitHub 仓库
2. 点击右上角的 **"Settings"**（设置）
3. 在左侧菜单中找到 **"Pages"**（页面）
4. 在 **"Source"**（源）部分：
   - 选择 **"Deploy from a branch"**（从分支部署）
   - 在 **"Branch"** 下拉菜单中选择 **"main"**（或您的主分支名称）
   - 在 **"Folder"** 下拉菜单中选择 **"/docs"**
5. 点击 **"Save"**（保存）

### 步骤 4：等待部署完成

- GitHub 通常需要 1-2 分钟来部署页面
- 您会看到一条消息："Your site is live at https://your-username.github.io/仓库名/"
- 或者访问：`https://your-username.github.io/仓库名/privacy-policy.html`

### 步骤 5：访问隐私政策

您的隐私政策地址将是：
```
https://您的GitHub用户名.github.io/仓库名/privacy-policy.html
```

**示例**：
- 如果您的用户名是 `raywaords`，仓库名是 `PhotoTimeGrouper`
- 地址就是：`https://raywaords.github.io/PhotoTimeGrouper/privacy-policy.html`

---

## 三、方法 2：使用 gh-pages 分支（备选）

如果您不想使用 `docs` 目录，可以使用 `gh-pages` 分支。

### 步骤 1：创建 gh-pages 分支

```bash
# 1. 创建并切换到 gh-pages 分支
git checkout -b gh-pages

# 2. 将隐私政策文件复制到根目录
# （将 docs/隐私政策.html 复制为 privacy-policy.html）

# 3. 添加文件
git add privacy-policy.html

# 4. 提交
git commit -m "Add privacy policy"

# 5. 推送到 GitHub
git push -u origin gh-pages

# 6. 切换回主分支
git checkout main
```

### 步骤 2：启用 GitHub Pages

1. 进入仓库 Settings → Pages
2. 在 Source 中选择 **"Deploy from a branch"**
3. Branch 选择 **"gh-pages"**
4. Folder 选择 **"/ (root)"**
5. 点击 Save

### 步骤 3：访问隐私政策

地址将是：
```
https://您的GitHub用户名.github.io/仓库名/privacy-policy.html
```

---

## 四、验证发布

### 4.1 检查页面是否可访问

1. 在浏览器中打开隐私政策 URL
2. 确认页面正常显示
3. 检查样式是否正确
4. 检查所有链接是否正常

### 4.2 测试 HTTPS

- ✅ 确认 URL 使用 HTTPS（GitHub Pages 自动提供）
- ✅ 确认没有安全警告

---

## 五、更新应用中的链接

### 5.1 找到 openPrivacyPolicy 方法

在 `app/src/main/java/com/phototimegrouper/app/MainActivity.kt` 中找到：

```kotlin
private fun openPrivacyPolicy() {
    // TODO: 将此处 URL 替换为您实际发布的隐私政策网页地址
    val privacyPolicyUrl = "https://your-domain.com/privacy-policy.html"
    // ...
}
```

### 5.2 更新 URL

将 URL 替换为您的实际 GitHub Pages 地址：

```kotlin
private fun openPrivacyPolicy() {
    val privacyPolicyUrl = "https://您的GitHub用户名.github.io/仓库名/privacy-policy.html"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(privacyPolicyUrl)
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            this,
            "无法打开隐私政策页面: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

**示例**：
```kotlin
val privacyPolicyUrl = "https://raywaords.github.io/PhotoTimeGrouper/privacy-policy.html"
```

### 5.3 测试应用中的链接

1. 运行应用
2. 点击右上角菜单 → "隐私政策"
3. 确认浏览器正确打开隐私政策页面

---

## 六、常见问题

### Q1: 页面显示 404 错误

**可能原因**：
- 文件路径不正确
- GitHub Pages 还未部署完成（等待 1-2 分钟）
- 分支或文件夹设置不正确

**解决方案**：
1. 检查文件是否在正确的位置
2. 确认 GitHub Pages 设置正确
3. 等待几分钟后重试
4. 检查 GitHub Pages 的部署状态（在 Settings → Pages 中查看）

### Q2: 页面样式丢失

**可能原因**：
- HTML 文件中的样式是内联的，应该没问题
- 如果使用了外部 CSS，需要确保路径正确

**解决方案**：
- 当前 `隐私政策.html` 使用内联样式，应该不会有问题
- 如果样式有问题，检查 HTML 文件内容

### Q3: 中文文件名问题

**解决方案**：
- ✅ 使用英文文件名：`privacy-policy.html`
- ❌ 避免使用中文：`隐私政策.html`

### Q4: 如何更新隐私政策

**步骤**：
1. 修改 `docs/privacy-policy.html` 文件
2. 提交更改到 GitHub
3. GitHub Pages 会自动更新（通常需要 1-2 分钟）

### Q5: 如何查看 GitHub Pages 部署状态

1. 进入仓库
2. 点击 "Actions" 标签
3. 查看 "pages build and deployment" 工作流
4. 确认部署成功（绿色勾号）

---

## 七、安全检查

### 7.1 确认 HTTPS

- ✅ GitHub Pages 自动提供 HTTPS
- ✅ 确保 URL 以 `https://` 开头

### 7.2 确认内容正确

- ✅ 检查隐私政策内容是否完整
- ✅ 检查联系信息是否正确
- ✅ 检查更新日期是否正确

---

## 八、完成检查清单

完成以下所有步骤后，隐私政策就发布成功了：

- [x] ✅ 隐私政策 HTML 文件已放到 `docs/` 目录
- [x] ✅ 文件已提交到 GitHub
- [x] ✅ GitHub Pages 已启用
- [ ] ⏳ 页面可以正常访问（等待1-2分钟部署完成）
- [x] ✅ 应用中的链接已更新
- [ ] ⏳ 在应用中测试链接正常工作（部署完成后测试）

---

## 九、下一步

隐私政策发布完成后，您可以：

1. ✅ 继续准备其他上架材料（应用图标、截图等）
2. ✅ 进行功能测试
3. ✅ 注册开发者账号
4. ✅ 提交应用上架

---

## 十、快速参考

### GitHub Pages 地址格式

```
https://您的GitHub用户名.github.io/仓库名/privacy-policy.html
```

### 常用命令

```bash
# 添加文件
git add docs/privacy-policy.html

# 提交
git commit -m "Add privacy policy"

# 推送
git push

# 查看 GitHub Pages 状态
# 在 GitHub 仓库 Settings → Pages 中查看
```

---

**文档版本**：1.0  
**最后更新**：2024年

**祝您发布顺利！** 🎉
