package com.rewardplatform.ticket.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 티켓 잔액 엔티티
 *
 * 게임 플레이, 랜덤박스 구매 등에 사용되는 2차 화폐.
 * 포인트와 달리 만료 정책이 단순하며, 게임 액션으로 빠르게 획득 가능.
 */
@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private int totalAmount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int availableAmount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int todayEarnedAmount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int todayUsedAmount = 0;

    private LocalDateTime lastEarnedDate;
    private LocalDateTime lastUsedDate;
    private LocalDateTime lastResetDate;

    public void earn(int amount) {
        this.totalAmount += amount;
        this.availableAmount += amount;
        this.todayEarnedAmount += amount;
        this.lastEarnedDate = LocalDateTime.now();
    }

    public void use(int amount) {
        if (this.availableAmount < amount) {
            throw new IllegalStateException("Insufficient tickets. Available: " + this.availableAmount);
        }
        this.availableAmount -= amount;
        this.todayUsedAmount += amount;
        this.lastUsedDate = LocalDateTime.now();
    }

    public void resetDailyStats() {
        this.todayEarnedAmount = 0;
        this.todayUsedAmount = 0;
        this.lastResetDate = LocalDateTime.now();
    }
}
