package com.rewardplatform.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 친구 초대 보상 이벤트
 *
 * 초대 완료 시 발행되며, TicketService와 PointService가 각각 리스닝합니다.
 * Spring ApplicationEvent를 사용한 도메인 간 느슨한 결합 구현.
 */
@Getter
@AllArgsConstructor
public class InviteRewardEvent {
    private final Long inviterId;
    private final Long inviteeId;
    private final int inviterTicketReward;
    private final int inviteeTicketReward;
    private final int inviterPointReward;
    private final int inviteePointReward;
}
