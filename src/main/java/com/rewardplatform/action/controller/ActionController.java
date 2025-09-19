package com.rewardplatform.action.controller;

import com.rewardplatform.action.dto.ActionPlayRequest;
import com.rewardplatform.action.dto.ActionPlayResult;
import com.rewardplatform.action.dto.AvailableActionResponse;
import com.rewardplatform.action.service.ActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Action", description = "게임 액션 API")
@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @Operation(summary = "참여 가능한 게임 목록", description = "일일 잔여 횟수 포함")
    @GetMapping("/{userId}/available")
    public ResponseEntity<List<AvailableActionResponse>> getAvailableActions(@PathVariable Long userId) {
        return ResponseEntity.ok(actionService.getAvailableActions(userId));
    }

    @Operation(summary = "게임 액션 수행", description = "게임 참여 및 보상 지급")
    @PostMapping("/{userId}/play")
    public ResponseEntity<ActionPlayResult> playAction(
            @PathVariable Long userId,
            @Valid @RequestBody ActionPlayRequest request) {
        return ResponseEntity.ok(actionService.playAction(userId, request));
    }
}
