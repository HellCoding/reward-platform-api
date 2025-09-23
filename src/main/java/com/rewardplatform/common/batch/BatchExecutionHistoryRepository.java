package com.rewardplatform.common.batch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BatchExecutionHistoryRepository extends JpaRepository<BatchExecutionHistory, Long> {

    Optional<BatchExecutionHistory> findTopByJobNameOrderByStartedAtDesc(String jobName);

    List<BatchExecutionHistory> findByJobNameAndStartedAtAfterOrderByStartedAtDesc(
            String jobName, LocalDateTime after);

    boolean existsByJobNameAndStatusAndStartedAtAfter(
            String jobName, BatchExecutionHistory.BatchStatus status, LocalDateTime after);
}
