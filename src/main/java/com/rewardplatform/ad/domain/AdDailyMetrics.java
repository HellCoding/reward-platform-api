package com.rewardplatform.ad.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 광고 플랫폼 일일 메트릭
 *
 * 플랫폼별 일일 광고 요청/노출 건수를 추적하여
 * Fill Rate = (impressions / requests) * 100 을 계산합니다.
 *
 * 일일 배치에서 이 데이터를 기반으로 미디에이션 우선순위를 재조정합니다.
 */
@Entity
@Table(name = "ad_daily_metrics", indexes = {
        @Index(name = "idx_adm_platform_date", columnList = "ad_platform_id, metric_date"),
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ad_platform_id", "metric_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdDailyMetrics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_platform_id", nullable = false)
    private AdPlatform adPlatform;

    @Column(nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    private long requests = 0;

    @Builder.Default
    private long impressions = 0;

    @Builder.Default
    private long clicks = 0;

    @Builder.Default
    private double revenue = 0.0; // USD

    public void incrementRequests() {
        this.requests++;
    }

    public void incrementImpressions() {
        this.impressions++;
    }

    public void incrementClicks() {
        this.clicks++;
    }

    public void addRevenue(double amount) {
        this.revenue += amount;
    }

    public double getFillRate() {
        return requests > 0 ? (double) impressions / requests * 100 : 0;
    }

    public double getCtr() {
        return impressions > 0 ? (double) clicks / impressions * 100 : 0;
    }

    public double getEcpm() {
        return impressions > 0 ? revenue / impressions * 1000 : 0;
    }
}
