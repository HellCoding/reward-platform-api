package com.rewardplatform.ad.service;

/**
 * 광고 시청 보상 전략 인터페이스 (Strategy Pattern)
 *
 * 광고 시청 후 다양한 보상 유형을 Strategy 패턴으로 구현:
 * - TicketRewardStrategy: 티켓 지급
 * - ExtraChanceRewardStrategy: 게임 추가 기회
 * - RewardBoostStrategy: 보상 배율 증가
 */
public interface AdRewardStrategy {
    void applyReward(Long userId, String adPlatform);
    String getRewardType();
}
