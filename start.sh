#!/bin/bash

# 简历投递管理系统启动脚本

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误：未找到Java环境，请安装JDK 17+"
    exit 1
fi

# 检查配置文件
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "警告：未找到配置文件，请先复制 application-example.properties 并配置数据库信息"
    echo "cp src/main/resources/application-example.properties src/main/resources/application.properties"
    exit 1
fi

# 设置环境变量（如果未设置）
if [ -z "$DB_URL" ]; then
    echo "警告：未设置DB_URL环境变量，将使用默认配置"
fi

if [ -z "$DB_USERNAME" ]; then
    echo "警告：未设置DB_USERNAME环境变量，将使用默认配置"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "警告：未设置DB_PASSWORD环境变量，将使用默认配置"
fi

if [ -z "$JWT_SECRET" ]; then
    echo "警告：未设置JWT_SECRET环境变量，将使用默认配置（不推荐生产环境）"
fi

# 启动应用
echo "正在启动简历投递管理系统..."
echo "访问地址：http://localhost:8080"
echo "默认管理员账号：root / admin123"
echo ""

java -jar target/resume-commit-*.jar
