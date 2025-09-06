package com.rewardplatform.point.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 포인트 트랜잭션 원장 (Source of Truth)
 *
 * 모든 포인트 변동은 이 테이블에 기록됩니다.
 * 가용 포인트 = SUM(EARN, 미만료) - SUM(USE) - SUM(EXPIRE)
 *
 * FIFO 기반 포인트 소진: 가장 오래된 EARN 트랜잭션부터 차감
 */
@Entity
@Table(name = "point_transactions", indexes = {
        @Index(name = "idx_pt_user_expiration", columnList = "user_id, expiration_date, state"),
        @Index(name = "idx_pt_user_type_state", columnList = "user_id, transaction_type, state"),
        @Index(name = "idx_pt_earned_date", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointTransaction extends BaseEntity {

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

    private String sourceId;
    private String description;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    @Builder.Default
    private int state = 1; // 1=active, 0=inactive

    private Long winningHistoryId;

    public enum TransactionType {
        EARN, USE, EXPIRE
    }

    public enum SourceType {
        RANDOM_BOX, DRAW, EVENT, ADMIN, INVITE, ACTION, EXPIRED
    }

    public boolean isActive() {
        return this.state == 1;
    }

    public boolean isExpired() {
        return this.expirationDate != null && this.expirationDate.isBefore(LocalDateTime.now());
    }

    public void deactivate() {
        this.state = 0;
    }
}
