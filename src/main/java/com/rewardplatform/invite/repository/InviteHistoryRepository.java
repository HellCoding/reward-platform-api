package com.rewardplatform.invite.repository;

import com.rewardplatform.invite.domain.InviteHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteHistoryRepository extends JpaRepository<InviteHistory, Long> {
    int countByInviterId(Long inviterId);
    Optional<InviteHistory> findByInviteeId(Long inviteeId);
}
