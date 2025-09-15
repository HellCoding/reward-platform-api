package com.rewardplatform.ad.repository;

import com.rewardplatform.ad.domain.AdPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdPlatformRepository extends JpaRepository<AdPlatform, Long> {
    List<AdPlatform> findByAdTypeAndIsActiveTrueOrderByPriorityAsc(AdPlatform.AdType adType);
}
