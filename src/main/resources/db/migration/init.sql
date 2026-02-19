-- 初始化数据库脚本

-- Agent 配置表
CREATE TABLE IF NOT EXISTS agent_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    system_prompt TEXT,
    tool_ids TEXT,
    max_iterations INT NOT NULL DEFAULT 10,
    context_window_size INT NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    metadata TEXT
);

-- 会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata TEXT
);

-- 消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_calls TEXT,
    tool_call_id VARCHAR(64),
    tool_name VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    sequence_number INT NOT NULL
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON chat_messages(created_at);

-- 插入示例 Agent 配置
INSERT INTO agent_configs (agent_id, name, system_prompt, tool_ids, max_iterations, context_window_size, enabled, created_at, updated_at)
VALUES (
    'math-assistant',
    'Math Assistant',
    'You are a helpful math assistant. You can perform calculations using the calculator tool. Always show your work and explain your reasoning step by step.',
    '["calculator"]',
    10,
    100,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO agent_configs (agent_id, name, system_prompt, tool_ids, max_iterations, context_window_size, enabled, created_at, updated_at)
VALUES (
    'general-assistant',
    'General Assistant',
    'You are a helpful AI assistant. You have access to various tools to help answer questions. Be concise but thorough in your responses.',
    '["calculator", "get_current_time"]',
    10,
    100,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
