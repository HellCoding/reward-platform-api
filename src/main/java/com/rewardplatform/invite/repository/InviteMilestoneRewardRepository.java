package com.rewardplatform.invite.repository;

import com.rewardplatform.invite.domain.InviteMilestoneReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteMilestoneRewardRepository extends JpaRepository<InviteMilestoneReward, Long> {
    boolean existsByUserIdAndInviteRewardId(Long userId, Long inviteRewardId);
}
