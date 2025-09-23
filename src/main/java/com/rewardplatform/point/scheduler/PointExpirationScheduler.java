package com.rewardplatform.point.scheduler;

import com.rewardplatform.common.batch.BatchExecutionManager;
import com.rewardplatform.common.batch.BatchExecutionManager.BatchResult;
import com.rewardplatform.point.domain.Point;
import com.rewardplatform.point.domain.PointTransaction;
import com.rewardplatform.point.domain.PointTransaction.SourceType;
import com.rewardplatform.point.domain.PointTransaction.TransactionType;
import com.rewardplatform.point.repository.PointRepository;
import com.rewardplatform.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 만료 배치 스케줄러
 *
 * BatchExecutionManager를 통해 리더 인스턴스에서만 실행되며,
 * 실행 이력이 자동으로 BatchExecutionHistory에 기록됩니다.
 *
 * 실행 스케줄:
 * - 00:00 만료 포인트 처리
 * - 00:30 비활성 사용자 포인트 만료
 * - 00:00 일일 통계 초기화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointExpirationScheduler {

    private final BatchExecutionManager batchManager;
    private final PointTransactionRepository transactionRepository;
    private final PointRepository pointRepository;

    @Value("${point.inactivity-threshold-days:90}")
    private int inactivityThresholdDays;

    private static final long LEADER_LEASE_MS = 300_000; // 5분

    /**
     * 만료 포인트 처리 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredPoints() {
        batchManager.executeIfLeader("point-expiration", LEADER_LEASE_MS, () -> {
            LocalDateTime now = LocalDateTime.now();
            List<PointTransaction> expired = transactionRepository.findExpiredEarnTransactions(now);

            int processed = 0;
            for (PointTransaction earnTx : expired) {
                try {
                    processExpiration(earnTx);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to expire transaction: {}", earnTx.getId(), e);
                }
            }

            return BatchResult.of(processed,
                    String.format("Expired %d/%d point transactions", processed, expired.size()));
        });
    }

    /**
     * 비활성 사용자 포인트 만료 (매일 00:30)
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void processInactiveUserPoints() {
        batchManager.executeIfLeader("inactive-user-expiration", LEADER_LEASE_MS, () -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(inactivityThresholdDays);

            List<Point> inactive = pointRepository.findAll().stream()
                    .filter(p -> p.getLastAccessDate() != null
                            && p.getLastAccessDate().isBefore(threshold)
                            && p.getAvailableAmount() > 0)
                    .toList();

            int processed = 0;
            for (Point point : inactive) {
                try {
                    int amount = point.getAvailableAmount();
                    point.expire(amount);

                    PointTransaction expireTx = PointTransaction.builder()
                            .user(point.getUser())
                            .amount(amount)
                            .transactionType(TransactionType.EXPIRE)
                            .sourceType(SourceType.EXPIRED)
                            .description("Inactive user (" + inactivityThresholdDays + " days)")
                            .build();

                    transactionRepository.save(expireTx);
                    pointRepository.save(point);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to expire inactive user: {}", point.getUser().getId(), e);
                }
            }

            return BatchResult.of(processed,
                    String.format("Expired %d inactive users' points", processed));
        });
    }

    @Transactional
    protected void processExpiration(PointTransaction earnTx) {
        earnTx.deactivate();

        PointTransaction expireTx = PointTransaction.builder()
                .user(earnTx.getUser())
                .amount(earnTx.getAmount())
                .transactionType(TransactionType.EXPIRE)
                .sourceType(SourceType.EXPIRED)
                .description("Point expired (earned: " + earnTx.getCreatedAt() + ")")
                .build();

        transactionRepository.save(expireTx);
    }
}
