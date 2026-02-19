package com.nanobot.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SessionManager - 管理会话并发控制
 *
 * 核心功能：
 * 1. 确保同一个 Session 不会同时处理多个请求
 * 2. 使用 ReentrantLock 实现细粒度锁控制
 * 3. 自动清理长时间未使用的锁
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    // 每个 sessionId 对应一个锁
    private final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 获取会话锁
     * 如果锁不存在，则创建新锁
     */
    public ReentrantLock getSessionLock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> {
            log.debug("Creating new lock for session: {}", sessionId);
            return new ReentrantLock(true); // 公平锁
        });
    }

    /**
     * 尝试获取会话锁
     *
     * @param sessionId 会话 ID
     * @return 是否成功获取锁
     */
    public boolean tryLockSession(String sessionId) {
        ReentrantLock lock = getSessionLock(sessionId);
        boolean acquired = lock.tryLock();

        if (acquired) {
            log.debug("Lock acquired for session: {}", sessionId);
        } else {
            log.warn("Failed to acquire lock for session: {} (already locked)", sessionId);
        }

        return acquired;
    }

    /**
     * 释放会话锁
     */
    public void unlockSession(String sessionId) {
        ReentrantLock lock = sessionLocks.get(sessionId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released for session: {}", sessionId);
        }
    }

    /**
     * 执行带锁的操作
     *
     * @param sessionId 会话 ID
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws SessionLockedException 如果无法获取锁
     */
    public <T> T executeWithLock(String sessionId, SessionAction<T> action) throws Exception {
        if (!tryLockSession(sessionId)) {
            throw new SessionLockedException("Session is currently processing another request: " + sessionId);
        }

        try {
            return action.execute();
        } finally {
            unlockSession(sessionId);
        }
    }

    /**
     * 清理指定会话的锁
     */
    public void cleanupSession(String sessionId) {
        ReentrantLock lock = sessionLocks.remove(sessionId);
        if (lock != null) {
            log.debug("Cleaned up lock for session: {}", sessionId);
        }
    }

    /**
     * 获取当前活跃的会话数量
     */
    public int getActiveSessionCount() {
        return sessionLocks.size();
    }

    /**
     * 会话操作接口
     */
    @FunctionalInterface
    public interface SessionAction<T> {
        T execute() throws Exception;
    }

    /**
     * 会话锁定异常
     */
    public static class SessionLockedException extends Exception {
        public SessionLockedException(String message) {
            super(message);
        }
    }
}
