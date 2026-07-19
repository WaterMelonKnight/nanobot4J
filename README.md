# Nanobot4J

> Experimental distributed Java Agent tool registration, discovery, remote invocation, and governance framework for Spring Boot services.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-green)](https://spring.io/projects/spring-boot)
[![Build](https://github.com/WaterMelonKnight/nanobot4J/actions/workflows/ci.yml/badge.svg)](https://github.com/WaterMelonKnight/nanobot4J/actions/workflows/ci.yml)

## Project status

Nanobot4J is a multi-module Java 17 / Spring Boot 3.2.2 experiment for registering tools from Spring Boot services, discovering those tools in an Admin service, invoking them remotely over HTTP, and driving a custom ReAct-style Agent loop.

The current codebase demonstrates production-oriented design goals such as service registration, heartbeat-based instance status, remote invocation, Redis-backed conversation storage, and Server-Sent Events (SSE) for Agent lifecycle events. It should not be presented as production-ready, enterprise-proven, industrial-grade, or benchmarked for production-scale workloads.

## Requirements

| Dependency | Version used by this repository | Notes |
| --- | --- | --- |
| JDK | 17 | `pom-parent.xml` sets `java.version`, `maven.compiler.source`, and `maven.compiler.target` to 17. |
| Maven | 3.8+ recommended | CI uses Maven with dependency caching. |
| Spring Boot | 3.2.2 | Managed by `pom-parent.xml`. |
| Redis | Optional at runtime for Admin memory features | `nanobot4j-admin` includes Spring Data Redis memory stores. |
| LLM API key | Optional at startup, required for real Agent responses | `llm.provider` supports the implemented DeepSeek and Kimi clients. |

## Modules

```text
nanobot4J/
├── nanobot4j-core/                 # Core abstractions: Agent, LLM messages/client DTOs, Tool, Memory
├── nanobot4j-spring-boot-starter/  # @NanobotTool scanning, local tool registry, Admin reporter, client execution endpoint
├── nanobot4j-admin/                # Admin service, registry, remote tool executor, custom ReAct agents, SSE, memory, dynamic Groovy tools
└── nanobot4j-example/              # Example Spring Boot service exposing a calculator tool
```

## Implemented capabilities

### Spring Boot tool registration

- `@NanobotTool` marks Spring bean methods as tools.
- `ToolScanner` discovers annotated methods after bean initialization and registers metadata in `ToolRegistry`.
- `AdminReporter` reports instance address and tool metadata to the Admin service on application startup and sends periodic heartbeats.
- `NanobotClientController` exposes the client-side HTTP endpoint used by Admin to execute a registered method.

### Admin registry and remote invocation

- `InstanceRegistry` stores reported service instances in memory, marks them `ONLINE`, and marks stale instances `OFFLINE` after a timeout.
- `RemoteToolExecutor` selects an online instance that advertises a requested tool and performs a synchronous HTTP POST to `/api/nanobot/client/execute`.

### Custom ReAct-style Agent loop

- `StreamingGenericReActAgent` implements a custom loop with a maximum of 15 steps.
- The loop calls `LLMService` synchronously for each step.
- It parses text markers such as `TOOL_CALL:` and `FINAL_ANSWER:` from model output. This is textual marker parsing, not native structured tool calling.
- It detects repeated failed tool calls with the same tool name and arguments and injects a warning event before continuing.
- It dispatches calls to the built-in `create_tool`, dynamic Groovy tools, or remote tools.

### Agent execution event streaming

The current SSE path streams Agent lifecycle events through Spring `SseEmitter`. Event types include:

- status/thinking messages;
- tool call;
- tool result;
- final answer;
- warning;
- error;
- completion/done.

This is Agent execution event streaming. The repository does not implement token-level LLM streaming, verified millisecond latency, million-level concurrent connections, actual chain-of-thought exposure guarantees, or a verified virtual-thread-per-connection architecture.

### Memory behavior

- Redis-backed chat message storage is implemented through `RedisChatMemoryStore`.
- Redis-backed summary storage is implemented through `RedisSummaryStore`.
- `MemorySummarizer` applies a sliding window of recent messages and can prepend an existing summary as a system message.
- When message count exceeds the threshold and no summary exists, `MemorySummarizer` asynchronously calls `LLMService` to create and store a summary.

No quantitative token-cost reduction or production effectiveness claim is made here because no benchmark is included.

## Experimental capabilities

### Dynamic Groovy tools

`ToolCreatorTool` can create a `DynamicGroovyTool` at runtime from a name, description, and Groovy script. `DynamicGroovyTool` executes scripts with bound parameters and a five-second timeout.

> **Security warning:** dynamic Groovy tool creation and execution is experimental and must not be enabled for untrusted input or exposed directly in production. The current implementation is not a secure sandbox: it does not provide process/runtime isolation, a strict capability allowlist, resource limits beyond a timeout, filesystem restrictions, network restrictions, or tests proving those boundaries.

## Known limitations

- Build reproducibility depends on the multi-module Maven parent `pom-parent.xml`; use `mvn -f pom-parent.xml ...` for full-repo builds.
- The Admin registry is in-memory; registrations are lost when the Admin process restarts.
- Remote tool invocation is synchronous HTTP and does not include retries, load balancing strategy, authentication, authorization, or circuit breaking.
- SSE emits lifecycle events from the Agent loop, not streamed model tokens.
- ReAct parsing depends on model adherence to textual markers. Malformed output may fail to parse.
- Dynamic Groovy execution is experimental and unsafe for untrusted code.
- Redis-backed memory requires a reachable Redis server for runtime use; tests in this repository do not prove production Redis behavior.
- The static HTML pages are demonstration pages, not a new management UI or a hardened operations console.

## Build and verification

From the repository root:

```bash
mvn -f pom-parent.xml clean test
mvn -f pom-parent.xml clean package -DskipTests
```

## Running locally

### 1. Clone

```bash
git clone https://github.com/WaterMelonKnight/nanobot4J.git
cd nanobot4J
```

### 2. Build

```bash
mvn -f pom-parent.xml clean package -DskipTests
```

### 3. Start the Admin service

Set whichever implemented provider you plan to use:

```bash
export DEEPSEEK_API_KEY=your_key_here
# or
export KIMI_API_KEY=your_key_here

mvn -pl nanobot4j-admin -am -f pom-parent.xml spring-boot:run
```

The Admin service defaults to port `8080`.

### 4. Start the example tool service

In another shell:

```bash
mvn -pl nanobot4j-example -am -f pom-parent.xml spring-boot:run
```

The example module demonstrates `@NanobotTool` registration from a Spring Boot service.

## Current roadmap

### Current implementation

- Java 17 multi-module Maven build.
- Spring Boot starter for annotation-based tool registration.
- Admin-side in-memory instance registry and HTTP remote tool execution.
- Custom synchronous ReAct-style loop with textual marker parsing.
- Agent lifecycle event streaming through SSE.
- Redis-backed chat memory and summary storage components.
- Experimental dynamic Groovy tool creation and execution.

### Recommended next milestone

- Add a separate Spring AI runtime adapter module that maps Nanobot4J tool metadata to Spring AI tool abstractions while preserving the existing starter/Admin architecture. This should be planned and implemented separately from this baseline documentation and CI task.

### Future work, not implemented now

- Hardened authentication/authorization for registration and remote invocation.
- Durable registry storage and health model.
- Structured tool calling adapter support.
- Tested sandboxing or removal of dynamic script execution from production paths.
- Integration tests for Admin + starter + example service.
- Observability, retries, backoff, and documented operational limits.

## Explicit non-goals for this baseline

This baseline does not add Spring AI, LangChain4j, SkillForge, a skill registry, MCP, Kubernetes sidecars, Go/Rust modules, WASM execution, multi-agent orchestration, a new management UI, new LLM providers, or an architectural rewrite.
