package com.rewardplatform.randombox.repository;

import com.rewardplatform.randombox.domain.WinningHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WinningHistoryRepository extends JpaRepository<WinningHistory, Long> {
    List<WinningHistory> findTop10ByOrderByCreatedAtDesc();
    List<WinningHistory> findByUserId(Long userId);
}
