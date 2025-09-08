package com.rewardplatform.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis 기반 분산 락 매니저
 *
 * ECS 클러스터 환경에서 동시성 제어를 위한 분산 락 구현.
 * - SET NX EX 기반 원자적 락 획득
 * - 지수 백오프 재시도 (50ms → 100ms → 200ms → 400ms → 800ms)
 * - UUID 기반 락 소유권 검증으로 안전한 해제
 * - 스케줄러 리더 선출 지원
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockManager {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "reward:lock:";
    private static final String LEADER_KEY = "reward:scheduler:leader";

    /**
     * 분산 락을 획득하고 작업 실행 후 락 해제
     *
     * @param lockKey    락 키
     * @param waitTimeMs 락 획득 대기 시간 (ms)
     * @param leaseTimeMs 락 유지 시간 (ms)
     * @param task       실행할 작업
     * @return 작업 결과
     */
    public <T> T executeWithLock(String lockKey, long waitTimeMs, long leaseTimeMs, Supplier<T> task) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();

        try {
            boolean acquired = acquireLockWithRetry(fullKey, lockValue, waitTimeMs, leaseTimeMs);
            if (!acquired) {
                throw new IllegalStateException("Failed to acquire lock: " + lockKey);
            }
            log.debug("Lock acquired: {}", lockKey);
            return task.get();
        } finally {
            releaseLock(fullKey, lockValue);
            log.debug("Lock released: {}", lockKey);
        }
    }

    /**
     * 분산 락을 획득하고 작업 실행 (반환값 없음)
     */
    public void executeWithLock(String lockKey, long waitTimeMs, long leaseTimeMs, Runnable task) {
        executeWithLock(lockKey, waitTimeMs, leaseTimeMs, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 지수 백오프 재시도를 사용한 락 획득
     * 재시도 간격: 50ms → 100ms → 200ms → 400ms → 800ms
     */
    private boolean acquireLockWithRetry(String key, String value, long waitTimeMs, long leaseTimeMs) {
        long deadline = System.currentTimeMillis() + waitTimeMs;
        long retryInterval = 50; // 초기 재시도 간격 50ms

        while (System.currentTimeMillis() < deadline) {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, Duration.ofMillis(leaseTimeMs));

            if (Boolean.TRUE.equals(result)) {
                return true;
            }

            try {
                Thread.sleep(retryInterval);
                retryInterval = Math.min(retryInterval * 2, 800); // 지수 백오프, 최대 800ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Lock acquisition timeout: key={}, waitTime={}ms", key, waitTimeMs);
        return false;
    }

    /**
     * 락 해제 (소유권 검증)
     * 자신이 설정한 락만 해제하여 다른 인스턴스의 락을 실수로 해제하지 않음
     */
    private void releaseLock(String key, String expectedValue) {
        String currentValue = redisTemplate.opsForValue().get(key);
        if (expectedValue.equals(currentValue)) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 스케줄러 리더 선출
     * ECS 다중 인스턴스 환경에서 하나의 인스턴스만 배치 작업 실행
     */
    public boolean tryBecomeLeader(String jobName, long leaseTimeMs) {
        String key = LEADER_KEY + ":" + jobName;
        String instanceId = getInstanceId();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, instanceId, Duration.ofMillis(leaseTimeMs));

        if (Boolean.TRUE.equals(acquired)) {
            log.info("Leader elected for job '{}': {}", jobName, instanceId);
            return true;
        }

        String currentLeader = redisTemplate.opsForValue().get(key);
        if (instanceId.equals(currentLeader)) {
            // 리더 갱신
            redisTemplate.expire(key, Duration.ofMillis(leaseTimeMs));
            return true;
        }

        log.debug("Not leader for job '{}'. Current leader: {}", jobName, currentLeader);
        return false;
    }

    /**
     * 락 존재 여부 확인
     */
    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + lockKey));
    }

    private String getInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        return hostname != null ? hostname : UUID.randomUUID().toString().substring(0, 8);
    }
}
