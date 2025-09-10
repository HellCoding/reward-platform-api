package com.rewardplatform.ticket.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 티켓 트랜잭션 원장
 * FIFO 기반 티켓 소진을 위한 remainingAmount 추적
 */
@Entity
@Table(name = "ticket_transactions", indexes = {
        @Index(name = "idx_tt_user_type", columnList = "user_id, transaction_type"),
        @Index(name = "idx_tt_status", columnList = "user_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TicketTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    private String description;

    private LocalDateTime expirationDate;

    @Column(nullable = false)
    @Builder.Default
    private int remainingAmount = 0; // FIFO 추적용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.ACTIVE;

    private Long relatedTransactionId;

    public enum TransactionType {
        EARN, USE
    }

    public enum SourceType {
        ACTION, EVENT, INVITE, RANDOM_BOX, REVIEW, ADMIN, ATTENDANCE, EXPIRED, AD, TIME_MISSION
    }

    public enum Status {
        ACTIVE, EXPIRED, USED
    }

    public void consumePartially(int consumeAmount) {
        this.remainingAmount -= consumeAmount;
        if (this.remainingAmount <= 0) {
            this.status = Status.USED;
        }
    }
}
