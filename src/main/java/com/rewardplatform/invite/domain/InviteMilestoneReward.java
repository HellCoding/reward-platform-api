package com.rewardplatform.invite.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite_milestone_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InviteMilestoneReward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_reward_id", nullable = false)
    private InviteReward inviteReward;

    @Column(nullable = false)
    private LocalDateTime achievedDate;
}
