package com.rewardplatform.ad.service;

import com.rewardplatform.ad.domain.AdPlatform;
import com.rewardplatform.ad.domain.AdPlatform.AdType;
import com.rewardplatform.ad.dto.AdMediationResponse;
import com.rewardplatform.ad.repository.AdPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 광고 미디에이션 서비스
 *
 * 3대 광고 플랫폼(AdMob, Unity Ads, Vungle)의 우선순위를 관리하고
 * Fill Rate 기반으로 동적 우선순위를 최적화합니다.
 *
 * 미디에이션 전략:
 * 1. 우선순위 순으로 광고 플랫폼 반환
 * 2. Fill Rate가 낮은 플랫폼은 자동으로 우선순위 강등
 * 3. 일일 배치로 메트릭 기반 우선순위 재조정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdMediationService {

    private final AdPlatformRepository adPlatformRepository;
    private final Map<String, AdRewardStrategy> rewardStrategies;

    /**
     * 미디에이션 광고 정보 조회
     * 우선순위 순으로 활성 광고 플랫폼 반환
     */
    @Transactional(readOnly = true)
    public List<AdMediationResponse> getMediationAds(AdType adType) {
        return adPlatformRepository.findByAdTypeAndIsActiveTrueOrderByPriorityAsc(adType)
                .stream()
                .map(AdMediationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 광고 시청 완료 처리 + 보상 지급
     */
    @Transactional
    public void processAdCompletion(Long userId, Long adPlatformId, String rewardType) {
        AdPlatform platform = adPlatformRepository.findById(adPlatformId)
                .orElseThrow(() -> new IllegalArgumentException("Ad platform not found: " + adPlatformId));

        platform.incrementImpressions();
        adPlatformRepository.save(platform);

        // Strategy 패턴으로 보상 지급
        AdRewardStrategy strategy = rewardStrategies.get(rewardType.toLowerCase() + "RewardStrategy");
        if (strategy != null) {
            strategy.applyReward(userId, platform.getPlatformName());
        } else {
            log.warn("Unknown reward type: {}", rewardType);
        }
    }

    /**
     * Fill Rate 기반 우선순위 최적화 (일일 배치)
     *
     * Fill Rate 높은 플랫폼이 더 높은 우선순위를 가지도록 자동 조정.
     * Fill Rate = 광고 노출 성공 / 광고 요청 * 100
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Transactional
    public void optimizeMediationPriorities() {
        log.info("Starting ad mediation priority optimization...");

        List<AdType> adTypes = List.of(AdType.REWARDED, AdType.INTERSTITIAL, AdType.BANNER);

        for (AdType adType : adTypes) {
            List<AdPlatform> platforms = adPlatformRepository
                    .findByAdTypeAndIsActiveTrueOrderByPriorityAsc(adType);

            // Fill Rate 기준 내림차순 정렬
            List<AdPlatform> sorted = platforms.stream()
                    .sorted(Comparator.comparingDouble(AdPlatform::getFillRate).reversed())
                    .collect(Collectors.toList());

            // 우선순위 재배정
            for (int i = 0; i < sorted.size(); i++) {
                AdPlatform platform = sorted.get(i);
                int newPriority = i + 1;
                if (platform.getPriority() != newPriority) {
                    log.info("Priority changed: {} {} → {} (fillRate: {}%)",
                            platform.getPlatformName(), platform.getPriority(), newPriority, platform.getFillRate());
                    platform.setPriority(newPriority);
                }
            }

            adPlatformRepository.saveAll(sorted);
        }

        log.info("Ad mediation priority optimization completed.");
    }
}
