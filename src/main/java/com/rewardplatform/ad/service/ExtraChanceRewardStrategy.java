package com.rewardplatform.ad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 광고 시청 → 게임 추가 기회 전략
 */
@Component
@Slf4j
public class ExtraChanceRewardStrategy implements AdRewardStrategy {

    @Override
    public void applyReward(Long userId, String adPlatform) {
        // 추가 기회는 ActionService에서 처리
        log.info("Extra chance reward applied - userId: {}, platform: {}", userId, adPlatform);
    }

    @Override
    public String getRewardType() {
        return "EXTRA_CHANCE";
    }
}
