# 修复 Gradle Wrapper
$wrapperJarUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
$wrapperJarPath = "gradle\wrapper\gradle-wrapper.jar"

Write-Host "正在下载 gradle-wrapper.jar..."
Write-Host "URL: $wrapperJarUrl"
Write-Host "保存到: $wrapperJarPath"

try {
    # 确保目录存在
    $wrapperDir = Split-Path -Parent $wrapperJarPath
    if (-not (Test-Path $wrapperDir)) {
        New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    }
    
    # 下载文件
    Invoke-WebRequest -Uri $wrapperJarUrl -OutFile $wrapperJarPath -UseBasicParsing
    Write-Host "✅ 下载成功！"
    
    # 验证文件
    if (Test-Path $wrapperJarPath) {
        $fileSize = (Get-Item $wrapperJarPath).Length
        Write-Host "文件大小: $fileSize 字节"
        Write-Host "✅ Gradle Wrapper 已修复，现在可以运行 ./gradlew test"
    } else {
        Write-Host "❌ 文件下载失败"
    }
} catch {
    Write-Host "❌ 下载失败: $($_.Exception.Message)"
    Write-Host ""
    Write-Host "备用方案："
    Write-Host "1. 访问 https://github.com/gradle/gradle/releases/tag/v8.5.0"
    Write-Host "2. 下载 gradle-wrapper.jar"
    Write-Host "3. 放置到 gradle\wrapper\ 目录"
}
