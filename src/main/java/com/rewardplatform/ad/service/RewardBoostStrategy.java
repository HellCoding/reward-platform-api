package com.rewardplatform.ad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 광고 시청 → 보상 배율 증가 전략
 *
 * 광고 시청 시 다음 게임 보상에 1.5배 배율을 적용합니다.
 * Redis에 TTL 30분으로 boost 상태를 저장하며, ActionService가 이를 참조합니다.
 */
@Component
@Slf4j
public class RewardBoostStrategy implements AdRewardStrategy {

    private final StringRedisTemplate redisTemplate;
    private static final String BOOST_KEY_PREFIX = "reward:boost:user:";
    private static final double BOOST_MULTIPLIER = 1.5;
    private static final Duration BOOST_DURATION = Duration.ofMinutes(30);

    public RewardBoostStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void applyReward(Long userId, String adPlatform) {
        String key = BOOST_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(BOOST_MULTIPLIER), BOOST_DURATION);
        log.info("Reward boost applied - userId: {}, multiplier: {}x, duration: {}min, platform: {}",
                userId, BOOST_MULTIPLIER, BOOST_DURATION.toMinutes(), adPlatform);
    }

    @Override
    public String getRewardType() {
        return "REWARD_BOOST";
    }

    /**
     * 사용자의 현재 보상 배율 조회
     */
    public double getBoostMultiplier(Long userId) {
        String value = redisTemplate.opsForValue().get(BOOST_KEY_PREFIX + userId);
        return value != null ? Double.parseDouble(value) : 1.0;
    }
}
