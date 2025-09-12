package com.rewardplatform.randombox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RandomBoxPurchaseResult {
    private Long winningHistoryId;
    private String prizeName;
    private int pointsAwarded;
    private int ticketCost;
    private boolean isWinner;
}
