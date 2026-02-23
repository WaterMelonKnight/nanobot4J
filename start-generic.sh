#!/bin/bash

# Nanobot4J 泛型 Agent 启动脚本（支持真实LLM）

echo "=========================================="
echo "  🤖 Nanobot4J Generic ReAct Agent"
echo "  基于 DeepSeek 的智能对话系统"
echo "=========================================="
echo ""

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "❌ 错误: 未找到 .env 文件"
    echo "请创建 .env 文件并配置以下内容："
    echo ""
    echo "DEEPSEEK_API_KEY=your-deepseek-api-key"
    echo "KIMI_API_KEY=your-kimi-api-key"
    echo ""
    exit 1
fi

# 加载环境变量
echo "📋 加载环境变量..."
export $(cat .env | grep -v '^#' | xargs)

if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "⚠️  警告: DEEPSEEK_API_KEY 未设置"
fi

if [ -z "$KIMI_API_KEY" ]; then
    echo "⚠️  警告: KIMI_API_KEY 未设置"
fi

echo "✅ 环境变量加载完成"
echo ""

# 1. 构建所有模块
echo "=========================================="
echo "Step 1: 构建所有模块..."
echo "=========================================="
mvn clean install -DskipTests -f pom-parent.xml

if [ $? -ne 0 ]; then
    echo "❌ 构建失败!"
    exit 1
fi

echo "✅ 构建成功!"
echo ""

# 2. 启动 Admin 控制台（带环境变量）
echo "=========================================="
echo "Step 2: 启动 Admin 控制台..."
echo "=========================================="
echo "🌐 Admin 地址: http://localhost:8080"
echo "🎨 泛型Agent页面: http://localhost:8080/chat-generic.html"
echo "🤖 LLM Provider: DeepSeek"
echo ""

cd nanobot4j-admin
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080" > /tmp/admin.log 2>&1 &
ADMIN_PID=$!

echo "✅ Admin Console PID: $ADMIN_PID"
echo "📝 日志文件: /tmp/admin.log"
echo "⏳ 等待 Admin 启动..."
sleep 15

# 检查 Admin 是否启动成功
if ! ps -p $ADMIN_PID > /dev/null; then
    echo "❌ Admin 启动失败，请查看日志: tail -f /tmp/admin.log"
    exit 1
fi

echo "✅ Admin 启动成功"
echo ""

# 3. 启动示例应用（Client）
echo "=========================================="
echo "Step 3: 启动 Client 应用..."
echo "=========================================="
echo "🔧 Client 地址: http://localhost:8081"
echo "📦 注册工具: calculator, weather, time"
echo ""

cd ../nanobot4j-example
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" > /tmp/client.log 2>&1 &
EXAMPLE_PID=$!

echo "✅ Client Application PID: $EXAMPLE_PID"
echo "📝 日志文件: /tmp/client.log"
echo "⏳ 等待 Client 启动并注册..."
sleep 15

# 检查 Client 是否启动成功
if ! ps -p $EXAMPLE_PID > /dev/null; then
    echo "❌ Client 启动失败，请查看日志: tail -f /tmp/client.log"
    kill $ADMIN_PID 2>/dev/null
    exit 1
fi

echo "✅ Client 启动成功"
echo ""

# 4. 验证服务状态
echo "=========================================="
echo "Step 4: 验证服务状态..."
echo "=========================================="

sleep 3

# 检查 Admin 健康状态
ADMIN_STATUS=$(curl -s http://localhost:8080/api/registry/instances | grep -o '"success":true' || echo "")
if [ -n "$ADMIN_STATUS" ]; then
    echo "✅ Admin 服务正常"
else
    echo "⚠️  Admin 服务可能未完全启动"
fi

# 检查工具注册
TOOLS_COUNT=$(curl -s http://localhost:8080/api/registry/instances | grep -o '"name":"[^"]*"' | wc -l)
echo "✅ 已注册工具数量: $TOOLS_COUNT"

echo ""

# 5. 完成
echo "=========================================="
echo "🎉 所有服务启动成功!"
echo "=========================================="
echo ""
echo "📊 服务信息:"
echo "  • Admin Dashboard: http://localhost:8080"
echo "  • 泛型Agent对话: http://localhost:8080/chat-generic.html"
echo "  • Client 应用: http://localhost:8081"
echo ""
echo "🤖 LLM 配置:"
echo "  • Provider: DeepSeek"
echo "  • Model: deepseek-chat"
echo ""
echo "🔧 可用工具:"
echo "  • calculator - 数学计算"
echo "  • weather - 天气查询"
echo "  • time - 时间查询"
echo ""
echo "📝 日志查看:"
echo "  • Admin: tail -f /tmp/admin.log"
echo "  • Client: tail -f /tmp/client.log"
echo ""
echo "🛑 停止服务:"
echo "  • kill $ADMIN_PID $EXAMPLE_PID"
echo "  • 或使用: pkill -f 'nanobot4j'"
echo ""
echo "💡 测试建议:"
echo "  1. 打开浏览器访问: http://localhost:8080/chat-generic.html"
echo "  2. 尝试问: '上海的天气怎么样？'"
echo "  3. 观察完整的 ReAct 流程"
echo ""
echo "=========================================="
