package com.rewardplatform.invite.repository;

import com.rewardplatform.invite.domain.InviteReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InviteRewardRepository extends JpaRepository<InviteReward, Long> {
    List<InviteReward> findAllByOrderByMilestoneSortingAsc();
}
