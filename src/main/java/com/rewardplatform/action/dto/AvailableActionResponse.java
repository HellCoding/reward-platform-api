package com.rewardplatform.action.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AvailableActionResponse {
    private Long actionId;
    private String actionName;
    private String actionType;
    private int successReward;
    private int failReward;
    private int dailyLimit;
    private int remainingAttempts;
    private int todayEarnedReward;
    private int maxDailyReward;
}
