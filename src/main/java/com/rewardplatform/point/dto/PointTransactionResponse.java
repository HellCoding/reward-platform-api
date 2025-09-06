package com.rewardplatform.point.dto;

import com.rewardplatform.point.domain.PointTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PointTransactionResponse {
    private Long id;
    private int amount;
    private String transactionType;
    private String sourceType;
    private String description;
    private LocalDateTime expirationDate;
    private LocalDateTime createdAt;

    public static PointTransactionResponse from(PointTransaction tx) {
        return PointTransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .transactionType(tx.getTransactionType().name())
                .sourceType(tx.getSourceType().name())
                .description(tx.getDescription())
                .expirationDate(tx.getExpirationDate())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
