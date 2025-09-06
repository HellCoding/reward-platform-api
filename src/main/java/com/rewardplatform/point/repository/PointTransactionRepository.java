package com.rewardplatform.point.repository;

import com.rewardplatform.point.domain.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /**
     * 가용 포인트 계산: 유효한 EARN 트랜잭션 합계
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.transactionType = 'EARN' " +
            "AND pt.expirationDate > :now AND pt.state = 1")
    int sumActiveEarnedPoints(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 사용된 포인트 합계
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.transactionType = 'USE' AND pt.state = 1")
    int sumUsedPoints(@Param("userId") Long userId);

    /**
     * 만료된 포인트 합계
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.transactionType = 'EXPIRE' AND pt.state = 1")
    int sumExpiredPoints(@Param("userId") Long userId);

    /**
     * 만료 예정 포인트 조회 (30일 이내)
     */
    @Query("SELECT pt FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.transactionType = 'EARN' " +
            "AND pt.expirationDate >= :now AND pt.expirationDate <= :threshold " +
            "AND pt.state = 1 ORDER BY pt.expirationDate ASC")
    List<PointTransaction> findExpiringPoints(@Param("userId") Long userId,
                                               @Param("now") LocalDateTime now,
                                               @Param("threshold") LocalDateTime threshold);

    /**
     * 배치 처리: 만료된 EARN 트랜잭션 조회
     */
    @Query("SELECT pt FROM PointTransaction pt " +
            "WHERE pt.transactionType = 'EARN' AND pt.expirationDate <= :now " +
            "AND pt.state = 1")
    List<PointTransaction> findExpiredEarnTransactions(@Param("now") LocalDateTime now);

    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
