# 检查并修复 gradle-wrapper.jar 的脚本

Write-Host "=== 检查 gradle-wrapper.jar ===" -ForegroundColor Cyan
Write-Host ""

$projectWrapper = "gradle\wrapper\gradle-wrapper.jar"
$currentSize = 0

if (Test-Path $projectWrapper) {
    $currentSize = (Get-Item $projectWrapper).Length
    Write-Host "当前文件大小: $currentSize 字节" -ForegroundColor Yellow
}

if ($currentSize -lt 50000) {
    Write-Host "文件异常，尝试修复..." -ForegroundColor Yellow
    Write-Host ""
    
    # 方法1: 从已下载的 Gradle 中查找
    Write-Host "方法1: 检查已下载的 Gradle..." -ForegroundColor Cyan
    $gradleDirs = Get-ChildItem -Path "$env:USERPROFILE\.gradle\wrapper\dists" -Recurse -Filter "gradle-8.5" -Directory -ErrorAction SilentlyContinue
    
    $found = $false
    foreach ($dir in $gradleDirs) {
        $wrapperJar = Join-Path $dir.FullName "gradle\wrapper\gradle-wrapper.jar"
        if (Test-Path $wrapperJar) {
            $file = Get-Item $wrapperJar
            Write-Host "  找到: $($file.FullName)"
            Write-Host "  大小: $($file.Length) 字节"
            if ($file.Length -gt 60000) {
                Copy-Item $wrapperJar -Destination $projectWrapper -Force
                Write-Host "  ✅ 已复制到项目！" -ForegroundColor Green
                $found = $true
                break
            }
        }
    }
    
    if (-not $found) {
        Write-Host "  未找到可用的 wrapper jar" -ForegroundColor Red
        Write-Host ""
        Write-Host "=== 无法自动修复 ===" -ForegroundColor Red
        Write-Host ""
        Write-Host "建议的解决方案：" -ForegroundColor Yellow
        Write-Host "1. 使用代理/VPN 后从 GitHub 下载"
        Write-Host "2. 请朋友帮忙下载文件"
        Write-Host "3. 使用手机热点重试下载"
        Write-Host "4. 暂时使用 GitHub Actions 自动修复（已配置）"
        Write-Host ""
        Write-Host "GitHub 下载地址：" -ForegroundColor Cyan
        Write-Host "  https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    }
} else {
    Write-Host "✅ 文件大小正常，无需修复" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== 检查完成 ===" -ForegroundColor Cyan
