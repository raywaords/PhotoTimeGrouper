@echo off
echo ========================================
echo 从 Gradle 缓存复制正确的 wrapper jar
echo ========================================
echo.

set GRADLE_CACHE=C:\Users\leiyu\.gradle\wrapper\dists\gradle-8.5-bin\5t9huq95ubn472n8rpzujfbqh\gradle-8.5
set WRAPPER_JAR=%GRADLE_CACHE%\gradle\wrapper\gradle-wrapper.jar
set TARGET=C:\Users\leiyu\Desktop\PhotoTimeGrouper\gradle\wrapper\gradle-wrapper.jar

echo 检查源文件...
if not exist "%WRAPPER_JAR%" (
    echo 错误: 未找到源文件
    echo 路径: %WRAPPER_JAR%
    echo.
    echo 请确认 Gradle 8.5 已完整下载
    pause
    exit /b 1
)

echo 找到源文件: %WRAPPER_JAR%
for %%A in ("%WRAPPER_JAR%") do (
    echo 文件大小: %%~zA 字节
)

echo.
echo 复制到项目目录...
copy /Y "%WRAPPER_JAR%" "%TARGET%"

if %errorlevel% == 0 (
    echo.
    echo ========================================
    echo 复制成功！
    echo ========================================
    echo.
    echo 目标文件: %TARGET%
    for %%A in ("%TARGET%") do (
        echo 文件大小: %%~zA 字节
    )
    echo.
    echo 现在可以运行: gradlew --version
) else (
    echo.
    echo ========================================
    echo 复制失败！
    echo ========================================
)

echo.
pause
