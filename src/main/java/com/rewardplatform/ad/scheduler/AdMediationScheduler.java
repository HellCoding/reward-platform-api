package com.rewardplatform.ad.scheduler;

import com.rewardplatform.ad.domain.AdPlatform;
import com.rewardplatform.ad.domain.AdPlatform.AdType;
import com.rewardplatform.ad.repository.AdDailyMetricsRepository;
import com.rewardplatform.ad.repository.AdPlatformRepository;
import com.rewardplatform.common.batch.BatchExecutionManager;
import com.rewardplatform.common.batch.BatchExecutionManager.BatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 광고 미디에이션 우선순위 최적화 배치
 *
 * 매일 새벽 3시에 실행되어, 전날 Fill Rate를 기반으로
 * 각 광고 플랫폼의 미디에이션 우선순위를 자동 재조정합니다.
 *
 * Fill Rate = (노출 성공 / 광고 요청) * 100
 * → 높은 Fill Rate = 높은 우선순위
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdMediationScheduler {

    private final BatchExecutionManager batchManager;
    private final AdPlatformRepository platformRepository;
    private final AdDailyMetricsRepository metricsRepository;

    private static final long LEADER_LEASE_MS = 300_000;

    @Scheduled(cron = "0 0 3 * * *")
    public void optimizeMediationPriorities() {
        batchManager.executeIfLeader("ad-mediation-optimization", LEADER_LEASE_MS, () -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<AdType> adTypes = List.of(AdType.REWARDED, AdType.INTERSTITIAL, AdType.BANNER);

            int totalChanged = 0;

            for (AdType adType : adTypes) {
                List<AdPlatform> platforms = platformRepository
                        .findByAdTypeAndIsActiveTrueOrderByPriorityAsc(adType);

                // 전일 메트릭 기반 Fill Rate 업데이트
                for (AdPlatform platform : platforms) {
                    metricsRepository.findByAdPlatformAndMetricDate(platform, yesterday)
                            .ifPresent(metrics -> {
                                platform.updateMetrics(metrics.getFillRate(), metrics.getCtr());
                            });
                }

                // Fill Rate 기준 내림차순 정렬 → 우선순위 재배정
                List<AdPlatform> sorted = platforms.stream()
                        .sorted(Comparator.comparingDouble(AdPlatform::getFillRate).reversed())
                        .toList();

                for (int i = 0; i < sorted.size(); i++) {
                    AdPlatform platform = sorted.get(i);
                    int newPriority = i + 1;
                    if (platform.getPriority() != newPriority) {
                        log.info("Priority changed: {} [{}] {} → {} (fillRate: {}%)",
                                platform.getPlatformName(), adType,
                                platform.getPriority(), newPriority, platform.getFillRate());
                        platform.setPriority(newPriority);
                        totalChanged++;
                    }
                }

                platformRepository.saveAll(sorted);
            }

            return BatchResult.of(totalChanged,
                    String.format("Optimized ad priorities, %d changes", totalChanged));
        });
    }
}
