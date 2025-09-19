package com.rewardplatform.action.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActionPlayResult {
    private String actionType;
    private boolean success;
    private int earnedReward;
    private int remainingAttempts;
}
