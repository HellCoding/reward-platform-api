package com.rewardplatform.randombox.domain;

import com.rewardplatform.common.domain.BaseEntity;
import com.rewardplatform.common.util.AbstractPrizeSelector;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 랜덤박스-상품 매핑 (확률 설정)
 *
 * 각 상품의 당첨 확률, 재고, 기간을 관리.
 * AbstractPrizeSelector.PrizeWithProbability 구현으로 확률 엔진과 연동.
 */
@Entity
@Table(name = "random_box_prizes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RandomBoxPrize extends BaseEntity implements AbstractPrizeSelector.PrizeWithProbability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id", nullable = false)
    private Prize prize;

    @Column(nullable = false)
    private Double winningProbability;

    private String displayProbability;

    private Integer winningPeriodDays;
    private Integer totalWinningCount;
    private Integer remainingCount;
    private LocalDateTime winningStartDate;

    // 포인트 상품용 범위 설정
    private Integer minPointValue;
    private Integer maxPointValue;

    @Column(nullable = false)
    @Builder.Default
    private boolean socialProofEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushEnabled = false;

    // --- PrizeWithProbability 인터페이스 구현 ---

    @Override
    public Double getWinningProbability() {
        return this.winningProbability;
    }

    @Override
    public Integer getRemainingCount() {
        return this.remainingCount;
    }

    @Override
    public void decrementRemainingCount() {
        if (this.remainingCount != null && this.remainingCount > 0) {
            this.remainingCount--;
        }
    }

    @Override
    public String getPrizeName() {
        return this.prize != null ? this.prize.getName() : "Unknown";
    }

    /**
     * 당첨 가능 여부 확인 (재고 + 기간)
     */
    public boolean isWinningAvailable() {
        if (remainingCount != null && remainingCount <= 0) return false;
        if (winningStartDate != null && winningStartDate.isAfter(LocalDateTime.now())) return false;
        return true;
    }

    /**
     * 포인트 상품 여부
     */
    public boolean isBasicPointPrize() {
        return prize != null && prize.getPrizeType() == Prize.PrizeType.POINT;
    }

    public boolean hasPointRange() {
        return minPointValue != null && maxPointValue != null && maxPointValue >= minPointValue;
    }
}
