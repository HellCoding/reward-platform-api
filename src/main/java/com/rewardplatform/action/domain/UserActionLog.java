package com.rewardplatform.action.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 유저 게임 액션 기록
 * 일일 참여 횟수, 보상 총액 추적
 */
@Entity
@Table(name = "user_action_logs", indexes = {
        @Index(name = "idx_ual_user_action_date", columnList = "user_id, action_id, participation_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserActionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private int earnedReward;

    @Column(nullable = false)
    private LocalDate participationDate;
}
