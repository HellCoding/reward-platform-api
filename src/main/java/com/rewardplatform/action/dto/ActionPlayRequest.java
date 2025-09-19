package com.rewardplatform.action.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActionPlayRequest {
    @NotNull
    private Long actionId;
    private boolean success;
}
