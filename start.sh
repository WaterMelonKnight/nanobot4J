#!/bin/bash

# Nanobot4J 快速启动脚本

echo "=========================================="
echo "  Nanobot4J Multi-Module Quick Start"
echo "=========================================="
echo ""

# 1. 构建所有模块
echo "Step 1: Building all modules..."
mvn clean install -DskipTests -f pom-parent.xml

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"
echo ""

# 2. 启动 Admin 控制台
echo "Step 2: Starting Admin Console..."
echo "Admin will be available at: http://localhost:8080"
echo ""

cd nanobot4j-admin
mvn spring-boot:run &
ADMIN_PID=$!

echo "Admin Console PID: $ADMIN_PID"
echo "Waiting for Admin to start..."
sleep 10

# 3. 启动示例应用
echo ""
echo "Step 3: Starting Example Application..."
echo "Example app will register with Admin automatically"
echo ""

cd ../nanobot4j-example
mvn spring-boot:run &
EXAMPLE_PID=$!

echo "Example Application PID: $EXAMPLE_PID"
echo ""

# 4. 完成
echo "=========================================="
echo "✅ All services started successfully!"
echo "=========================================="
echo ""
echo "📊 Admin Dashboard: http://localhost:8080"
echo "🔧 Example App: http://localhost:8081"
echo ""
echo "To stop services:"
echo "  kill $ADMIN_PID $EXAMPLE_PID"
echo ""
echo "Or use: pkill -f 'nanobot4j'"
echo ""
