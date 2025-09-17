package com.rewardplatform.ad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 광고 시청 → 랜덤박스 재도전 기회 전략
 *
 * 랜덤박스에서 꽝이 나왔을 때 광고를 시청하면 1회 무료 재도전 기회를 부여합니다.
 * Redis에 TTL 10분으로 retry 토큰을 저장합니다.
 */
@Component
@Slf4j
public class DrawRetryRewardStrategy implements AdRewardStrategy {

    private final StringRedisTemplate redisTemplate;
    private static final String RETRY_KEY_PREFIX = "reward:draw-retry:user:";
    private static final Duration RETRY_DURATION = Duration.ofMinutes(10);

    public DrawRetryRewardStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void applyReward(Long userId, String adPlatform) {
        String key = RETRY_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "1", RETRY_DURATION);
        log.info("Draw retry token granted - userId: {}, platform: {}", userId, adPlatform);
    }

    @Override
    public String getRewardType() {
        return "DRAW_RETRY";
    }

    /**
     * 재도전 토큰 사용 (1회 소모)
     */
    public boolean consumeRetryToken(Long userId) {
        String key = RETRY_KEY_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(key);
        if (token != null) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
