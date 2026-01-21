# 修复 gradle-wrapper.jar 文件（用于 GitHub Actions）

Write-Host "=== 修复 gradle-wrapper.jar ===" -ForegroundColor Cyan
Write-Host ""

$wrapperPath = "gradle\wrapper\gradle-wrapper.jar"
$backupPath = "gradle\wrapper\gradle-wrapper.jar.backup"

# 备份现有文件
if (Test-Path $wrapperPath) {
    Write-Host "备份现有文件..." -ForegroundColor Yellow
    Copy-Item $wrapperPath $backupPath -Force
    $oldSize = (Get-Item $wrapperPath).Length
    $oldSizeKB = [math]::Round($oldSize/1KB, 2)
    Write-Host "原文件大小: $oldSize 字节 ($oldSizeKB KB)" -ForegroundColor Yellow
}

# 尝试从多个源下载
$sources = @(
    "https://repo.maven.apache.org/maven2/org/gradle/gradle-wrapper/8.5/gradle-wrapper-8.5.jar",
    "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
)

$success = $false
foreach ($url in $sources) {
    Write-Host ""
    Write-Host "尝试从以下地址下载: $url" -ForegroundColor Cyan
    try {
        Invoke-WebRequest -Uri $url -OutFile $wrapperPath -ErrorAction Stop
        $newSize = (Get-Item $wrapperPath).Length
        $newSizeKB = [math]::Round($newSize/1KB, 2)
        Write-Host "下载成功！文件大小: $newSize 字节 ($newSizeKB KB)" -ForegroundColor Green
        
        # 验证文件大小（应该是 60-70 KB）
        if ($newSize -gt 60000 -and $newSize -lt 80000) {
            Write-Host "文件大小正常！" -ForegroundColor Green
            $success = $true
            break
        } else {
            Write-Host "警告: 文件大小异常，可能不完整" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "下载失败: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

if (-not $success) {
    Write-Host ""
    Write-Host "自动下载失败。请尝试其他方法：" -ForegroundColor Red
    Write-Host "1. 从其他正常工作的 Android 项目复制 gradle-wrapper.jar" -ForegroundColor Yellow
    Write-Host "2. 或使用 Android Studio 打开项目（会自动修复）" -ForegroundColor Yellow
    Write-Host ""
    
    # 恢复备份
    if (Test-Path $backupPath) {
        Write-Host "恢复备份文件..." -ForegroundColor Yellow
        Copy-Item $backupPath $wrapperPath -Force
    }
    exit 1
}

# 验证文件大小
Write-Host ""
Write-Host "验证文件..." -ForegroundColor Cyan
$finalSize = (Get-Item $wrapperPath).Length
$finalSizeKB = [math]::Round($finalSize/1KB, 2)
if ($finalSize -gt 60000 -and $finalSize -lt 80000) {
    Write-Host "文件大小正常 ($finalSizeKB KB)，验证通过！" -ForegroundColor Green
} else {
    Write-Host "警告: 文件大小异常 ($finalSizeKB KB)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 修复完成 ===" -ForegroundColor Green
Write-Host "下一步操作:" -ForegroundColor Cyan
Write-Host "  git add gradle/wrapper/gradle-wrapper.jar" -ForegroundColor White
Write-Host "  git commit -m 'fix: 修复 gradle-wrapper.jar 文件'" -ForegroundColor White
Write-Host "  git push origin main" -ForegroundColor White
