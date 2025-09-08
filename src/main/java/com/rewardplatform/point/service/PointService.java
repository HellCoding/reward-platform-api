package com.rewardplatform.point.service;

import com.rewardplatform.common.util.DistributedLockManager;
import com.rewardplatform.point.domain.Point;
import com.rewardplatform.point.domain.PointTransaction;
import com.rewardplatform.point.domain.PointTransaction.SourceType;
import com.rewardplatform.point.domain.PointTransaction.TransactionType;
import com.rewardplatform.point.dto.PointStatusResponse;
import com.rewardplatform.point.dto.PointTransactionResponse;
import com.rewardplatform.point.repository.PointRepository;
import com.rewardplatform.point.repository.PointTransactionRepository;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 관리 서비스
 *
 * 트랜잭션 원장(PointTransaction) 기반으로 포인트를 관리합니다.
 * 가용 포인트 = SUM(EARN, 미만료) - SUM(USE) - SUM(EXPIRE)
 *
 * 핵심 설계:
 * - FIFO 기반 포인트 소진 (가장 오래된 적립부터 차감)
 * - 365일 기본 만료 정책
 * - Redis 분산 락으로 동시성 제어
 *
 * 배치 처리는 PointExpirationScheduler로 분리되어
 * BatchExecutionManager + LeaderElectionService를 통해 관리됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final PointRepository pointRepository;
    private final PointTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DistributedLockManager lockManager;

    @Value("${point.default-expiration-days:365}")
    private int defaultExpirationDays;

    /**
     * 포인트 현황 조회 (트랜잭션 원장 기반)
     */
    @Transactional(readOnly = true)
    public PointStatusResponse getPointStatus(Long userId) {
        Point point = getOrCreatePoint(userId);
        int available = calculateAvailablePoints(userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysLater = now.plusDays(30);
        List<PointTransaction> expiringPoints = transactionRepository
                .findExpiringPoints(userId, now, thirtyDaysLater);

        int expiringAmount = expiringPoints.stream()
                .mapToInt(PointTransaction::getAmount)
                .sum();

        return PointStatusResponse.builder()
                .userId(userId)
                .totalEarned(point.getTotalAmount())
                .availablePoints(available)
                .todayEarned(point.getTodayEarnedAmount())
                .todayUsed(point.getTodayUsedAmount())
                .expiringIn30Days(expiringAmount)
                .lastAccessDate(point.getLastAccessDate())
                .build();
    }

    /**
     * 가용 포인트 계산 (트랜잭션 원장 기반)
     * available = SUM(EARN, 미만료) - SUM(USE) - SUM(EXPIRE)
     */
    @Transactional(readOnly = true)
    public int calculateAvailablePoints(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        int earned = transactionRepository.sumActiveEarnedPoints(userId, now);
        int used = transactionRepository.sumUsedPoints(userId);
        int expired = transactionRepository.sumExpiredPoints(userId);
        return Math.max(0, earned - used - expired);
    }

    /**
     * 포인트 적립
     * Redis 분산 락으로 동시성 제어
     */
    @Transactional
    public PointTransaction earnPoints(Long userId, int amount, SourceType sourceType,
                                        String sourceId, String description) {
        String lockKey = "point:user:" + userId;
        return lockManager.executeWithLock(lockKey, 5000, 30000, () -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Point point = getOrCreatePoint(userId);
            point.earn(amount);

            LocalDateTime expirationDate = LocalDateTime.now().plusDays(defaultExpirationDays);

            PointTransaction transaction = PointTransaction.builder()
                    .user(user)
                    .amount(amount)
                    .transactionType(TransactionType.EARN)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .description(description)
                    .expirationDate(expirationDate)
                    .build();

            transactionRepository.save(transaction);
            pointRepository.save(point);

            log.info("Points earned - userId: {}, amount: {}, source: {}", userId, amount, sourceType);
            return transaction;
        });
    }

    /**
     * 포인트 사용
     * REPEATABLE_READ 격리 수준으로 트랜잭션 정합성 보장
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PointTransaction usePoints(Long userId, int amount, SourceType sourceType,
                                       String sourceId, String description) {
        String lockKey = "point:user:" + userId;
        return lockManager.executeWithLock(lockKey, 5000, 30000, () -> {
            int available = calculateAvailablePoints(userId);
            if (available < amount) {
                throw new IllegalStateException(
                        "Insufficient points. Available: " + available + ", Requested: " + amount);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            Point point = getOrCreatePoint(userId);
            point.use(amount);

            PointTransaction transaction = PointTransaction.builder()
                    .user(user)
                    .amount(amount)
                    .transactionType(TransactionType.USE)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .description(description)
                    .build();

            transactionRepository.save(transaction);
            pointRepository.save(point);

            log.info("Points used - userId: {}, amount: {}, remaining: {}",
                    userId, amount, point.getAvailableAmount());
            return transaction;
        });
    }

    /**
     * 트랜잭션 이력 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PointTransactionResponse::from);
    }

    private Point getOrCreatePoint(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return pointRepository.save(Point.builder().user(user).build());
                });
    }
}
