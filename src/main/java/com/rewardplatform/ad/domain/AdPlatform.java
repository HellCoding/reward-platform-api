package com.rewardplatform.ad.domain;

import com.rewardplatform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 광고 플랫폼 설정 엔티티
 *
 * AdMob, Unity Ads, Vungle 등 광고 플랫폼별 설정 관리.
 * 우선순위 기반 미디에이션으로 최적의 광고 플랫폼 선택.
 */
@Entity
@Table(name = "ad_platforms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdPlatform extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platformName; // ADMOB, UNITY_ADS, VUNGLE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdType adType; // REWARDED, INTERSTITIAL, BANNER

    @Column(nullable = false)
    private int priority; // 낮을수록 높은 우선순위

    private String adUnitIdAndroid;
    private String adUnitIdIos;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // 성과 메트릭
    @Builder.Default
    private double fillRate = 0.0;

    @Builder.Default
    private double ctr = 0.0;

    @Builder.Default
    private long totalImpressions = 0;

    @Builder.Default
    private long totalClicks = 0;

    public enum AdType {
        REWARDED, INTERSTITIAL, BANNER
    }

    public void updateMetrics(double fillRate, double ctr) {
        this.fillRate = fillRate;
        this.ctr = ctr;
    }

    public void incrementImpressions() {
        this.totalImpressions++;
    }

    public void incrementClicks() {
        this.totalClicks++;
        this.ctr = totalImpressions > 0 ? (double) totalClicks / totalImpressions * 100 : 0;
    }
}
