package com.rewardplatform.invite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InviteStatusResponse {
    private String inviteCode;
    private int totalInvites;
    private int nextMilestone;
    private int invitesUntilNextMilestone;
}
