# Nanobot4J 启动故障排查指南

## 🔴 常见错误及解决方案

### 错误 1: Spring AI 依赖无法解析

**错误信息**:
```
[ERROR] Non-resolvable import POM: org.springframework.ai:spring-ai-bom:pom:1.0.0-M4 was not found
[ERROR] 'dependencies.dependency.version' for org.springframework.ai:spring-ai-openai-spring-boot-starter:jar is missing
```

**原因**:
- Spring AI 1.0.0-M4 是里程碑版本，可能在某些 Maven 仓库中不可用
- 需要添加 Spring Milestones 仓库

**解决方案**:

#### 方案 1: 清理 Maven 缓存并强制更新
```bash
# 清理 Maven 本地缓存
rm -rf ~/.m2/repository/org/springframework/ai

# 强制更新依赖
mvn clean install -U
```

#### 方案 2: 修改 pom.xml 使用稳定版本
如果 Spring AI 1.0.0-M4 无法下载，可以降级到更稳定的版本或移除 Spring AI 依赖，使用我们的多模型架构。

---

### 错误 2: 数据库初始化失败

**错误信息**:
```
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException
```

**原因**: 缺少必要的 Repository 或 Entity 类

**解决方案**: 检查以下文件是否存在
- `AgentConfigRepository.java`
- `ChatSessionRepository.java`
- `ChatMessageRepository.java`
- `AgentConfig.java` (Entity)
- `ChatSession.java` (Entity)

---

### 错误 3: LLM 客户端初始化失败

**错误信息**:
```
No qualifying bean of type 'com.nanobot.llm.LLMClient' available
```

**原因**: LLMClient Bean 未正确配置

**解决方案**: 确保以下配置类存在
- `MultiModelConfig.java`
- `ModelProviderFactory.java`

---

## 🚀 快速修复步骤

### 步骤 1: 清理并重新编译
```bash
mvn clean
mvn compile
```

### 步骤 2: 检查 Java 版本
```bash
java -version
# 应该是 Java 21
```

### 步骤 3: 检查配置文件
确保 `application.yml` 配置正确：
```yaml
nanobot:
  llm:
    default-model: ollama
    models:
      ollama:
        enabled: true
        provider: ollama
        base-url: http://localhost:11434
        model: bitnet
```

### 步骤 4: 简化启动（跳过测试）
```bash
mvn spring-boot:run -DskipTests
```

---

## 🔧 临时解决方案：移除 Spring AI 依赖

如果 Spring AI 依赖一直无法解析，可以暂时注释掉相关依赖：

1. 打开 `pom.xml`
2. 注释掉以下部分：
```xml
<!-- 暂时注释
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
-->
```

3. 注释掉 dependencyManagement 部分：
```xml
<!-- 暂时注释
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
-->
```

4. 重新编译：
```bash
mvn clean compile
```

---

## 📋 完整的故障排查清单

- [ ] Java 版本是否为 21
- [ ] Maven 版本是否 >= 3.6
- [ ] 网络连接是否正常
- [ ] Maven 仓库配置是否正确
- [ ] 本地 Maven 缓存是否损坏
- [ ] 所有必需的 Java 文件是否存在
- [ ] application.yml 配置是否正确
- [ ] 端口 8080 是否被占用

---

## 🆘 如果还是无法启动

请提供以下信息：

1. **完整的错误日志**:
```bash
mvn spring-boot:run > error.log 2>&1
cat error.log
```

2. **Java 版本**:
```bash
java -version
```

3. **Maven 版本**:
```bash
mvn -version
```

4. **检查端口占用**:
```bash
lsof -i :8080
# 或
netstat -an | grep 8080
```

---

## 💡 推荐的启动流程

```bash
# 1. 清理项目
mvn clean

# 2. 编译（查看编译错误）
mvn compile

# 3. 如果编译成功，启动应用
mvn spring-boot:run

# 4. 如果启动失败，查看详细日志
mvn spring-boot:run -X
```

---

## 📞 需要帮助？

如果遇到具体错误，请：
1. 复制完整的错误信息
2. 说明你执行的命令
3. 提供 Java 和 Maven 版本信息

我会帮你快速定位和解决问题！
