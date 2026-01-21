@echo off
echo ========================================
echo 修复 Gradle Wrapper
echo ========================================
echo.

cd /d "%~dp0"

echo 检查 gradle-wrapper.jar 文件...
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo 文件已存在，跳过下载
    goto :verify
)

echo.
echo 正在下载 gradle-wrapper.jar...
echo.

REM 尝试使用 curl (Windows 10+)
where curl >nul 2>&1
if %errorlevel% == 0 (
    echo 使用 curl 下载...
    curl -L -o "gradle\wrapper\gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    if %errorlevel% == 0 (
        echo 下载成功！
        goto :verify
    )
)

REM 尝试使用 PowerShell
echo 使用 PowerShell 下载...
powershell -Command "try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar' -UseBasicParsing; Write-Host 'Download successful' } catch { Write-Host 'Download failed, please download manually' }"

:verify
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo.
    echo ========================================
    echo 验证文件...
    echo ========================================
    for %%A in ("gradle\wrapper\gradle-wrapper.jar") do (
        echo 文件大小: %%~zA 字节
        if %%~zA LSS 50000 (
            echo 警告: 文件大小异常，可能下载不完整
        ) else (
            echo 文件大小正常
        )
    )
    echo.
    echo ========================================
    echo 修复完成！现在可以运行: gradlew test
    echo ========================================
) else (
    echo.
    echo ========================================
    echo 下载失败！
    echo ========================================
    echo.
    echo 请手动下载 gradle-wrapper.jar:
    echo 1. 访问: https://github.com/gradle/gradle/releases/tag/v8.5.0
    echo 2. 或直接下载: https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar
    echo 3. 保存到: gradle\wrapper\gradle-wrapper.jar
    echo.
    echo 详细说明请查看: docs\修复GradleWrapper问题.md
    echo.
)

pause
