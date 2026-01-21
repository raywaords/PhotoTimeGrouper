@echo off
chcp 65001 >nul
echo ========================================
echo 使用本地 Gradle 生成 gradle-wrapper.jar
echo ========================================
echo.

REM 检查是否在项目根目录
if not exist "gradle\wrapper\gradle-wrapper.properties" (
    echo 错误：请在项目根目录运行此脚本！
    echo 当前目录：%CD%
    pause
    exit /b 1
)

echo 请提供 Gradle 的 bin 目录路径
echo 例如：D:\迅雷下载\gradle-8.5-all\gradle-8.5\bin
echo.
set /p GRADLE_BIN="请输入 Gradle bin 目录路径: "

REM 检查 gradle.bat 是否存在
if not exist "%GRADLE_BIN%\gradle.bat" (
    echo.
    echo 错误：找不到 gradle.bat 文件！
    echo 路径：%GRADLE_BIN%\gradle.bat
    echo.
    echo 请确认：
    echo 1. 已解压 gradle-8.5-all.zip
    echo 2. 路径正确（应该指向 bin 目录）
    echo 3. bin 目录中有 gradle.bat 文件
    pause
    exit /b 1
)

echo.
echo 找到 gradle.bat，开始生成 wrapper...
echo.

REM 使用本地 Gradle 生成 wrapper
"%GRADLE_BIN%\gradle.bat" wrapper --gradle-version=8.5

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✓ 生成成功！
    echo ========================================
    echo.
    
    REM 检查文件
    if exist "gradle\wrapper\gradle-wrapper.jar" (
        for %%A in ("gradle\wrapper\gradle-wrapper.jar") do (
            set SIZE=%%~zA
        )
        echo 文件位置：gradle\wrapper\gradle-wrapper.jar
        echo 文件大小：%SIZE% 字节
        echo.
        
        if %SIZE% LSS 50000 (
            echo ⚠ 警告：文件大小可能不正常（应该约 60-70 KB）
        ) else (
            echo ✓ 文件大小正常
        )
    )
    
    echo.
    echo 正在测试...
    call gradlew --version
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ========================================
        echo ✓ 测试成功！wrapper 已正常工作
        echo ========================================
    ) else (
        echo.
        echo ⚠ 测试失败，但文件已生成
    )
) else (
    echo.
    echo ========================================
    echo ✗ 生成失败
    echo ========================================
    echo.
    echo 可能的原因：
    echo 1. 网络连接问题（需要下载 wrapper jar）
    echo 2. Gradle 版本不匹配
    echo 3. 权限问题
    echo.
    echo 建议：
    echo - 检查网络连接
    echo - 尝试用 Android Studio 打开项目（会自动修复）
    echo - 或从其他 Android 项目复制 gradle-wrapper.jar
)

echo.
pause
