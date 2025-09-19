package com.rewardplatform.action.repository;

import com.rewardplatform.action.domain.UserActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {

    @Query("SELECT COUNT(l) FROM UserActionLog l " +
            "WHERE l.user.id = :userId AND l.action.id = :actionId " +
            "AND l.participationDate = :date")
    int countTodayParticipation(@Param("userId") Long userId,
                                 @Param("actionId") Long actionId,
                                 @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(l.earnedReward), 0) FROM UserActionLog l " +
            "WHERE l.user.id = :userId AND l.action.id = :actionId " +
            "AND l.participationDate = :date")
    int sumTodayReward(@Param("userId") Long userId,
                        @Param("actionId") Long actionId,
                        @Param("date") LocalDate date);

    Page<UserActionLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
