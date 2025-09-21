package com.rewardplatform.invite.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 초대 마일스톤 보상 설정
 * 5명, 10명, 15명... 단위로 추가 보상 지급
 */
@Entity
@Table(name = "invite_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InviteReward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int milestoneSorting; // 5, 10, 15, 20...

    @Column(nullable = false)
    @Builder.Default
    private int ticketReward = 0;

    @Column(nullable = false)
    @Builder.Default
    private int pointReward = 0;
}
