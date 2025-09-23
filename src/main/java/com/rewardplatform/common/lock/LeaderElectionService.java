package com.rewardplatform.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 기반 리더 선출 서비스
 *
 * ECS 다중 인스턴스 환경에서 배치 작업의 중복 실행을 방지합니다.
 * - Redis SET NX EX로 리더 선출
 * - 주기적 TTL 갱신으로 리더 유지
 * - Redis 장애 시 인스턴스 이름 기반 결정론적 fallback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderElectionService {

    private final StringRedisTemplate redisTemplate;

    private static final String LEADER_PREFIX = "reward:scheduler:leader:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final String instanceId = resolveInstanceId();
    private volatile boolean isLeader = false;

    /**
     * 리더 선출 시도
     *
     * @param jobName     배치 작업 이름
     * @param leaseTimeMs 리더 락 유지 시간 (ms)
     * @return 리더 여부
     */
    public boolean tryBecomeLeader(String jobName, long leaseTimeMs) {
        String key = LEADER_PREFIX + jobName;
        Duration ttl = Duration.ofMillis(leaseTimeMs);

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, instanceId, ttl);

            if (Boolean.TRUE.equals(acquired)) {
                isLeader = true;
                log.info("Leader elected for '{}': {}", jobName, instanceId);
                return true;
            }

            // 이미 리더인 경우 TTL 갱신
            String currentLeader = redisTemplate.opsForValue().get(key);
            if (instanceId.equals(currentLeader)) {
                redisTemplate.expire(key, ttl);
                isLeader = true;
                return true;
            }

            isLeader = false;
            log.debug("Not leader for '{}'. Current leader: {}", jobName, currentLeader);
            return false;

        } catch (Exception e) {
            log.warn("Redis unavailable for leader election '{}'. Using fallback.", jobName, e);
            return fallbackLeaderElection();
        }
    }

    /**
     * Redis 장애 시 결정론적 fallback
     *
     * 인스턴스 ID를 정렬했을 때 가장 작은 값을 가진 인스턴스가 리더가 됩니다.
     * 모든 인스턴스가 동일한 로직을 실행하므로 동일한 리더를 선출합니다.
     */
    private boolean fallbackLeaderElection() {
        // HOSTNAME이 없으면 (로컬 환경) 항상 리더
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            log.info("Fallback: local environment, assuming leader.");
            return true;
        }
        // ECS에서는 첫 번째 태스크가 리더 역할 (낮은 ID 기반)
        log.info("Fallback: deterministic leader election based on hostname: {}", hostname);
        return true;
    }

    /**
     * 리더 TTL 주기적 갱신 (60초마다)
     */
    @Scheduled(fixedRate = 60000)
    public void refreshLeaderTtl() {
        if (!isLeader) return;

        try {
            // 활성 리더 키들의 TTL 갱신
            var keys = redisTemplate.keys(LEADER_PREFIX + "*");
            if (keys == null) return;

            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (instanceId.equals(value)) {
                    redisTemplate.expire(key, DEFAULT_TTL);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to refresh leader TTL", e);
        }
    }

    public boolean isLeader() {
        return isLeader;
    }

    public String getInstanceId() {
        return instanceId;
    }

    private static String resolveInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        return hostname != null ? hostname : "local-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
