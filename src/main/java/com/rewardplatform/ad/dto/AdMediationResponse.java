package com.rewardplatform.ad.dto;

import com.rewardplatform.ad.domain.AdPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdMediationResponse {
    private Long id;
    private String platformName;
    private String adType;
    private int priority;
    private String adUnitIdAndroid;
    private String adUnitIdIos;
    private double fillRate;

    public static AdMediationResponse from(AdPlatform platform) {
        return AdMediationResponse.builder()
                .id(platform.getId())
                .platformName(platform.getPlatformName())
                .adType(platform.getAdType().name())
                .priority(platform.getPriority())
                .adUnitIdAndroid(platform.getAdUnitIdAndroid())
                .adUnitIdIos(platform.getAdUnitIdIos())
                .fillRate(platform.getFillRate())
                .build();
    }
}
