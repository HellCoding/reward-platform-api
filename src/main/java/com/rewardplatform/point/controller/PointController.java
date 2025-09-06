package com.rewardplatform.point.controller;

import com.rewardplatform.point.dto.PointStatusResponse;
import com.rewardplatform.point.dto.PointTransactionResponse;
import com.rewardplatform.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Point", description = "포인트 관리 API")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 현황 조회", description = "트랜잭션 원장 기반 실시간 포인트 현황")
    @GetMapping("/{userId}/status")
    public ResponseEntity<PointStatusResponse> getPointStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(pointService.getPointStatus(userId));
    }

    @Operation(summary = "포인트 이력 조회", description = "포인트 트랜잭션 이력 (페이지네이션)")
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<Page<PointTransactionResponse>> getTransactions(
            @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(pointService.getTransactionHistory(userId, pageable));
    }
}
