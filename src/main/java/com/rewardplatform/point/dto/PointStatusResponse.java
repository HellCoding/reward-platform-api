package com.rewardplatform.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PointStatusResponse {
    private Long userId;
    private int totalEarned;
    private int availablePoints;
    private int todayEarned;
    private int todayUsed;
    private int expiringIn30Days;
    private LocalDateTime lastAccessDate;
}
