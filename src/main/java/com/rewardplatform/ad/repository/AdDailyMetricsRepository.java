package com.rewardplatform.ad.repository;

import com.rewardplatform.ad.domain.AdDailyMetrics;
import com.rewardplatform.ad.domain.AdPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdDailyMetricsRepository extends JpaRepository<AdDailyMetrics, Long> {

    Optional<AdDailyMetrics> findByAdPlatformAndMetricDate(AdPlatform platform, LocalDate date);

    List<AdDailyMetrics> findByMetricDate(LocalDate date);

    @Query("SELECT m FROM AdDailyMetrics m WHERE m.adPlatform = :platform " +
            "AND m.metricDate BETWEEN :startDate AND :endDate ORDER BY m.metricDate DESC")
    List<AdDailyMetrics> findByPlatformAndDateRange(
            @Param("platform") AdPlatform platform,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
