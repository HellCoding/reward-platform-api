package com.rewardplatform.invite.controller;

import com.rewardplatform.invite.dto.InviteStatusResponse;
import com.rewardplatform.invite.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invite", description = "친구 초대 API")
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @Operation(summary = "초대 코드 조회/생성")
    @GetMapping("/{userId}/code")
    public ResponseEntity<String> getInviteCode(@PathVariable Long userId) {
        return ResponseEntity.ok(inviteService.getOrCreateInviteCode(userId));
    }

    @Operation(summary = "초대 코드 사용")
    @PostMapping("/{userId}/redeem")
    public ResponseEntity<Void> redeemCode(@PathVariable Long userId, @RequestParam String code) {
        inviteService.redeemInviteCode(userId, code);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "초대 현황 조회")
    @GetMapping("/{userId}/status")
    public ResponseEntity<InviteStatusResponse> getInviteStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(inviteService.getInviteStatus(userId));
    }
}
