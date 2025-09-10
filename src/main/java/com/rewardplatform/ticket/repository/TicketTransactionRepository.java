package com.rewardplatform.ticket.repository;

import com.rewardplatform.ticket.domain.TicketTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {

    /**
     * FIFO 소진을 위한 잔여 EARN 트랜잭션 조회 (생성일 오름차순)
     */
    @Query("SELECT tt FROM TicketTransaction tt " +
            "WHERE tt.user.id = :userId AND tt.transactionType = 'EARN' " +
            "AND tt.status = 'ACTIVE' AND tt.remainingAmount > 0 " +
            "ORDER BY tt.createdAt ASC")
    List<TicketTransaction> findAvailableEarnTransactions(@Param("userId") Long userId);

    Page<TicketTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
