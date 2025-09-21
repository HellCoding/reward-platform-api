package com.rewardplatform.invite.repository;

import com.rewardplatform.invite.domain.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    Optional<InviteCode> findByCode(String code);
    Optional<InviteCode> findByUserId(Long userId);
}
