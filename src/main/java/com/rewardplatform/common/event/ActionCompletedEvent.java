package com.rewardplatform.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 게임 액션 완료 이벤트
 *
 * 게임 플레이 완료 시 발행되며, 보상 배율 적용 및 통계 수집에 사용됩니다.
 */
@Getter
@AllArgsConstructor
public class ActionCompletedEvent {
    private final Long userId;
    private final String actionType;
    private final boolean success;
    private final int ticketReward;
}
