package com.rewardplatform.randombox.dto;

import com.rewardplatform.randombox.domain.RandomBox;
import com.rewardplatform.randombox.domain.RandomBoxPrize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class RandomBoxDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String boxType;
    private int ticketCost;
    private String imageUrl;
    private List<PrizeInfo> prizes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PrizeInfo {
        private Long id;
        private String prizeName;
        private String displayProbability;
        private Integer remainingCount;
        private boolean socialProofEnabled;
    }

    public static RandomBoxDetailResponse from(RandomBox box) {
        List<PrizeInfo> prizeInfos = box.getPrizes().stream()
                .sorted(Comparator.comparingDouble(RandomBoxPrize::getWinningProbability).reversed())
                .map(rbp -> PrizeInfo.builder()
                        .id(rbp.getId())
                        .prizeName(rbp.getPrizeName())
                        .displayProbability(rbp.getDisplayProbability())
                        .remainingCount(rbp.getRemainingCount())
                        .socialProofEnabled(rbp.isSocialProofEnabled())
                        .build())
                .collect(Collectors.toList());

        return RandomBoxDetailResponse.builder()
                .id(box.getId())
                .name(box.getName())
                .description(box.getDescription())
                .boxType(box.getBoxType().name())
                .ticketCost(box.getTicketCost())
                .imageUrl(box.getImageUrl())
                .prizes(prizeInfos)
                .build();
    }
}
