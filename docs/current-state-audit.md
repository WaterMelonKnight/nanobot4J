# Current State Audit

## 1. Repository overview

Nanobot4J is currently a Java 17, Maven multi-module repository rooted at `pom-parent.xml`. The parent aggregates `nanobot4j-core`, `nanobot4j-spring-boot-starter`, `nanobot4j-admin`, and `nanobot4j-example`.

- `pom-parent.xml` defines Java 17 compiler settings and Spring Boot 3.2.2 dependency management.
- `nanobot4j-core` contains framework-neutral abstractions for Agents, LLM DTOs, tools, and memory.
- `nanobot4j-spring-boot-starter` contains annotation scanning, local tool registration, Admin reporting, auto-configuration, and the client execution controller.
- `nanobot4j-admin` contains the Admin application, in-memory service registry, remote tool executor, custom ReAct agents, SSE streaming controller, Redis memory components, and experimental dynamic Groovy tools.
- `nanobot4j-example` is a Spring Boot example service that exposes a calculator tool.

## 2. Verified implemented capabilities

### Multi-module Java 17 build

`pom-parent.xml` sets `java.version`, `maven.compiler.source`, and `maven.compiler.target` to `17`, and aggregates all four modules. Child modules now point their Maven parent resolution at `../pom-parent.xml` so `mvn -f pom-parent.xml ...` can build reproducibly from a clean checkout.

### Spring Boot tool registration

`NanobotAutoConfiguration` creates the starter beans: `ToolRegistry`, `ToolScanner`, conditional `AdminReporter`, and `NanobotClientController`.

`ToolScanner` implements `BeanPostProcessor`, inspects initialized Spring beans, finds methods annotated with `@NanobotTool`, and registers them with `ToolRegistry`.

`AdminReporter` listens for `ApplicationReadyEvent`, derives an instance ID and callback address, posts registration data to `/api/registry/register`, and schedules heartbeats to `/api/registry/beat`.

### Admin-side registry and remote execution

`InstanceRegistry` stores instances in a `ConcurrentHashMap`, marks registered instances `ONLINE`, and marks stale instances `OFFLINE` during a scheduled status check.

`RemoteToolExecutor` finds an online instance advertising the requested tool and synchronously posts execution requests to `/api/nanobot/client/execute`.

### Custom ReAct-style Agent loop and SSE lifecycle events

`StreamingGenericReActAgent` implements a custom ReAct-style loop with a 15-step maximum, synchronous calls to `LLMService.chat`, textual parsing of `TOOL_CALL:` and `FINAL_ANSWER:`, dispatch to built-in, dynamic, or remote tools, repeated failed-call detection, and `SseEmitter` event delivery.

`AgentStreamEvent` defines `THINKING`, `TOOL_CALL`, `TOOL_RESULT`, `FINAL_ANSWER`, `DONE`, `ERROR`, and `WARNING` events. `StreamAgentController` exposes the SSE endpoint and executes the Agent loop asynchronously on a cached thread pool.

### LLM service

`LLMService` selects the configured provider and includes synchronous HTTP implementations for DeepSeek and Kimi chat-completions-style APIs. No native streaming response handling is implemented there.

### Memory components

`RedisChatMemoryStore` and `RedisSummaryStore` implement Redis-backed storage components. `MemorySummarizer` uses a sliding window of 10 messages, prepends an existing summary when present, and asynchronously generates a summary through `LLMService` when the threshold is exceeded and no summary exists.

## 3. Partially implemented capabilities

- Service governance exists as in-memory registration, heartbeat updates, stale-instance marking, and remote HTTP invocation. It does not yet include authentication, durable registry storage, load balancing, retries, or production health semantics.
- SSE exists for Agent execution lifecycle events, but not token-level model streaming.
- ReAct-style execution exists, but structured tool calling is represented by textual markers parsed from model output, not native LLM tool-call APIs.
- Memory summarization components exist, but there are no benchmarks proving token-cost reduction or production effectiveness.

## 4. Experimental capabilities

Dynamic Groovy tools are experimental. `ToolCreatorTool` validates basic parameters, rejects a few obvious dangerous strings, creates `DynamicGroovyTool`, and registers it in `DynamicToolRegistry`. `DynamicGroovyTool` executes Groovy code with bound parameters in an executor and applies a five-second timeout.

This is not a secure sandbox. The code does not demonstrate process or runtime isolation, a strict capability allowlist, filesystem or network restrictions, comprehensive resource limits, or tests proving containment. It must not be enabled for untrusted input or exposed directly in production.

## 5. Unsupported or removed claims

The previous README overstated several areas that are not supported by the current implementation:

- Java 21 baseline: unsupported by the current parent build, which is Java 17.
- Production-ready, enterprise-proven, industrial-grade positioning: not supported by tests, benchmarks, or operational hardening in the repository.
- Verified Java 21 virtual-thread-per-SSE-connection architecture: not implemented; the SSE controller uses a cached thread pool.
- Million-level concurrency and millisecond latency claims: no benchmarks or implementation evidence.
- Token-level LLM streaming: `LLMService` makes synchronous chat calls and the SSE layer emits Agent events.
- Actual chain-of-thought exposure: the code parses `<thinking>` text generated by the model, but this should be described as lifecycle/status text rather than verified internal reasoning exposure.
- Secure Groovy sandbox: not demonstrated by the implementation.
- Quantitative token-cost reduction: no benchmark data is present.

## 6. Known technical risks

- Dynamic Groovy execution can execute arbitrary Groovy/JVM behavior beyond the limited string checks in `ToolCreatorTool`.
- The Admin registry is volatile and process-local.
- Remote tool invocation lacks authentication, authorization, retries, backoff, circuit breaking, and load balancing.
- The ReAct loop relies on model compliance with textual markers and may fail on malformed responses.
- Runtime memory behavior depends on Redis availability and configuration.
- There are limited or no automated tests exercising multi-process Admin/starter/example interactions.

## 7. Recommended next milestone

Implement a separate Spring AI runtime adapter module that maps Nanobot4J tool metadata to Spring AI tool abstractions and delegates execution through the existing registry/executor model. This should be a separate milestone and should not rewrite the current starter/Admin architecture.

## 8. Verification commands and results

Initial baseline command:

```bash
git status
```

Result: clean working tree on branch `work` before edits.

```bash
git log -5 --oneline
```

Result at inspection time:

```text
3cc44d0 fix: 配置忽略未知属性以避免反序列化错误
2c68325 fix: 调整默认端口及回调地址配置逻辑
f473ba6 docs: 重构项目文档架构和内容
6d31df0 chore: 移除 spring-boot-maven-plugin
9828c0b feat: 支持多轮记忆与防死循环的 ReAct 重构
```

```bash
mvn -f pom-parent.xml clean test
```

Initial result: failed because `nanobot4j-core`, `nanobot4j-spring-boot-starter`, and `nanobot4j-admin` declared parent `com.nanobot:nanobot4j-parent` but their implicit `../pom.xml` parent path pointed to `com.nanobot:nanobot4j` instead of `pom-parent.xml`.

Final verification results are recorded in the pull request/working-tree summary after the baseline fixes.
