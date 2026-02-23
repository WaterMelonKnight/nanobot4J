#!/bin/bash

# Nanobot4J 服务停止脚本

echo "=========================================="
echo "  🛑 停止 Nanobot4J 服务"
echo "=========================================="
echo ""

# 查找并停止所有相关进程
echo "🔍 查找运行中的服务..."
echo ""

ADMIN_PIDS=$(ps aux | grep 'AdminApplication' | grep -v grep | awk '{print $2}')
CLIENT_PIDS=$(ps aux | grep 'ExampleApplication' | grep -v grep | awk '{print $2}')

if [ -z "$ADMIN_PIDS" ] && [ -z "$CLIENT_PIDS" ]; then
    echo "ℹ️  没有找到运行中的服务"
    exit 0
fi

# 停止 Admin 服务
if [ -n "$ADMIN_PIDS" ]; then
    echo "🛑 停止 Admin 服务..."
    for pid in $ADMIN_PIDS; do
        echo "  • 终止进程: $pid"
        kill $pid 2>/dev/null
    done
    echo "✅ Admin 服务已停止"
else
    echo "ℹ️  Admin 服务未运行"
fi

echo ""

# 停止 Client 服务
if [ -n "$CLIENT_PIDS" ]; then
    echo "🛑 停止 Client 服务..."
    for pid in $CLIENT_PIDS; do
        echo "  • 终止进程: $pid"
        kill $pid 2>/dev/null
    done
    echo "✅ Client 服务已停止"
else
    echo "ℹ️  Client 服务未运行"
fi

echo ""

# 等待进程完全终止
echo "⏳ 等待进程完全终止..."
sleep 3

# 检查是否还有残留进程
REMAINING=$(ps aux | grep -E 'AdminApplication|ExampleApplication' | grep -v grep)
if [ -n "$REMAINING" ]; then
    echo "⚠️  发现残留进程，强制终止..."
    pkill -9 -f 'AdminApplication'
    pkill -9 -f 'ExampleApplication'
    sleep 2
fi

echo ""
echo "=========================================="
echo "✅ 所有服务已停止"
echo "=========================================="
echo ""
echo "📝 日志文件保留在:"
echo "  • /tmp/admin.log"
echo "  • /tmp/client.log"
echo ""
