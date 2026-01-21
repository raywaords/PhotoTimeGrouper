@echo off
echo ========================================
echo 生成 Gradle Wrapper
echo ========================================
echo.

cd /d "%~dp0"

echo 方法 1: 检查是否已安装 Gradle...
gradle --version >nul 2>&1
if %errorlevel% == 0 (
    echo 找到 Gradle，正在生成 wrapper...
    gradle wrapper --gradle-version=8.5 --distribution-type=bin
    if %errorlevel% == 0 (
        echo.
        echo ========================================
        echo 生成成功！
        echo ========================================
        goto :end
    )
)

echo.
echo 方法 2: 尝试使用 Android Studio 的 Gradle...
set ANDROID_STUDIO_GRADLE=
if exist "%LOCALAPPDATA%\Android\Sdk" (
    for /d %%d in ("%LOCALAPPDATA%\Android\Sdk\gradle\*") do (
        if exist "%%d\bin\gradle.bat" (
            set ANDROID_STUDIO_GRADLE=%%d\bin\gradle.bat
            goto :found
        )
    )
)

:found
if defined ANDROID_STUDIO_GRADLE (
    echo 找到 Android Studio 的 Gradle: %ANDROID_STUDIO_GRADLE%
    call "%ANDROID_STUDIO_GRADLE%" wrapper --gradle-version=8.5 --distribution-type=bin
    if %errorlevel% == 0 (
        echo.
        echo ========================================
        echo 生成成功！
        echo ========================================
        goto :end
    )
)

echo.
echo ========================================
echo 无法自动生成 wrapper
echo ========================================
echo.
echo 请尝试以下方法：
echo.
echo 1. 安装 Gradle: https://gradle.org/install/
echo 2. 或从其他 Android 项目复制 gradle/wrapper/gradle-wrapper.jar
echo 3. 或使用 Android Studio 打开项目，它会自动修复
echo.
echo 详细说明请查看: docs/GradleWrapper下载方案.md
echo.

:end
pause
