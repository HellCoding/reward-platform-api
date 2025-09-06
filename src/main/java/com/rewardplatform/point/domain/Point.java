package com.rewardplatform.point.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 포인트 잔액 관리 엔티티
 *
 * 실제 가용 포인트는 PointTransaction 원장 기반으로 계산됩니다.
 * availableAmount = SUM(EARN) - SUM(USE) - SUM(EXPIRE)
 *
 * 이 엔티티의 값은 캐시 역할이며, 트랜잭션 원장이 source of truth입니다.
 */
@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Point extends BaseEntity {

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
    private LocalDateTime lastAccessDate;

    public void earn(int amount) {
        this.totalAmount += amount;
        this.availableAmount += amount;
        this.todayEarnedAmount += amount;
        this.lastEarnedDate = LocalDateTime.now();
        this.lastAccessDate = LocalDateTime.now();
    }

    public void use(int amount) {
        if (this.availableAmount < amount) {
            throw new IllegalStateException("Insufficient points. Available: " + this.availableAmount + ", Requested: " + amount);
        }
        this.availableAmount -= amount;
        this.todayUsedAmount += amount;
        this.lastUsedDate = LocalDateTime.now();
        this.lastAccessDate = LocalDateTime.now();
    }

    public void expire(int amount) {
        this.availableAmount = Math.max(0, this.availableAmount - amount);
    }

    public void resetDailyStats() {
        this.todayEarnedAmount = 0;
        this.todayUsedAmount = 0;
        this.lastResetDate = LocalDateTime.now();
    }

    public void recalculateAvailable(int calculatedAmount) {
        this.availableAmount = Math.max(0, calculatedAmount);
    }

    public void updateAccessTime() {
        this.lastAccessDate = LocalDateTime.now();
    }
}
