@echo off
chcp 65001 >nul

REM 简历投递管理系统启动脚本 (Windows)

echo 正在检查Java环境...

REM 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误：未找到Java环境，请安装JDK 17+
    pause
    exit /b 1
)

REM 检查配置文件
if not exist "src\main\resources\application.properties" (
    echo 警告：未找到配置文件，请先复制 application-example.properties 并配置数据库信息
    echo copy src\main\resources\application-example.properties src\main\resources\application.properties
    pause
    exit /b 1
)

REM 检查环境变量
if "%DB_URL%"=="" (
    echo 警告：未设置DB_URL环境变量，将使用默认配置
)

if "%DB_USERNAME%"=="" (
    echo 警告：未设置DB_USERNAME环境变量，将使用默认配置
)

if "%DB_PASSWORD%"=="" (
    echo 警告：未设置DB_PASSWORD环境变量，将使用默认配置
)

if "%JWT_SECRET%"=="" (
    echo 警告：未设置JWT_SECRET环境变量，将使用默认配置（不推荐生产环境）
)

REM 启动应用
echo.
echo 正在启动简历投递管理系统...
echo 访问地址：http://localhost:8080
echo 默认管理员账号：root / admin123
echo.

REM 查找jar文件
for %%f in (target\resume-commit-*.jar) do (
    java -jar "%%f"
    goto :end
)

echo 错误：未找到编译后的jar文件，请先运行 mvn clean package
pause
exit /b 1

:end
pause
