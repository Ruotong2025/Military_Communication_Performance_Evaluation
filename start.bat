@echo off
echo ========================================
echo 军事通信效能评估系统 - 启动脚本
echo ========================================
echo.

echo [1/3] 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未检测到Java环境，请先安装JDK 17+
    pause
    exit /b 1
)
echo.

echo [2/3] 检查Maven环境...
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未检测到Maven环境，请先安装Maven 3.6+
    pause
    exit /b 1
)
echo.

echo [3/3] 启动Spring Boot应用...
echo.
mvn spring-boot:run

pause
