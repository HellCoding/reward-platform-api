package com.rewardplatform.randombox.service;

import com.rewardplatform.common.util.AbstractPrizeSelector;
import com.rewardplatform.common.util.DistributedLockManager;
import com.rewardplatform.point.domain.PointTransaction;
import com.rewardplatform.point.service.PointService;
import com.rewardplatform.randombox.domain.*;
import com.rewardplatform.randombox.dto.RandomBoxDetailResponse;
import com.rewardplatform.randombox.dto.RandomBoxPurchaseResult;
import com.rewardplatform.randombox.repository.RandomBoxRepository;
import com.rewardplatform.randombox.repository.WinningHistoryRepository;
import com.rewardplatform.ticket.service.TicketService;
import com.rewardplatform.ticket.domain.TicketTransaction;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 랜덤박스 서비스
 *
 * 확률 기반 상품 선택 엔진과 이중 화폐 시스템을 결합.
 * 1. 티켓 차감 → 2. 확률 엔진으로 상품 선택 → 3. 포인트/상품 지급 → 4. 이력 기록
 *
 * 소셜 프루프: 최근 당첨 내역을 노출하여 사용자 참여 유도
 */
@Service
@Slf4j
public class RandomBoxService extends AbstractPrizeSelector {

    private final RandomBoxRepository randomBoxRepository;
    private final WinningHistoryRepository winningHistoryRepository;
    private final TicketService ticketService;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final DistributedLockManager lockManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public RandomBoxService(RandomBoxRepository randomBoxRepository,
                            WinningHistoryRepository winningHistoryRepository,
                            TicketService ticketService,
                            PointService pointService,
                            UserRepository userRepository,
                            DistributedLockManager lockManager) {
        this.randomBoxRepository = randomBoxRepository;
        this.winningHistoryRepository = winningHistoryRepository;
        this.ticketService = ticketService;
        this.pointService = pointService;
        this.userRepository = userRepository;
        this.lockManager = lockManager;
    }

    /**
     * 활성 랜덤박스 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RandomBoxDetailResponse> getActiveBoxes() {
        return randomBoxRepository.findAllActiveWithPrizes().stream()
                .map(RandomBoxDetailResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 랜덤박스 구매 (핵심 로직)
     *
     * Flow:
     * 1. 티켓 잔액 확인 및 차감
     * 2. 확률 엔진으로 상품 선택
     * 3. 포인트 상품인 경우 범위 내 랜덤 포인트 지급
     * 4. 당첨 이력 기록
     */
    @Transactional
    public RandomBoxPurchaseResult purchaseBox(Long userId, Long boxId) {
        String lockKey = "reward:randombox:lock:" + boxId + ":user:" + userId;

        return lockManager.executeWithLock(lockKey, 5000, 30000, () -> {
            RandomBox box = randomBoxRepository.findById(boxId)
                    .orElseThrow(() -> new IllegalArgumentException("Box not found: " + boxId));

            if (!box.isActive()) {
                throw new IllegalStateException("Box is not active");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 1. 티켓 차감
            ticketService.useTickets(userId, box.getTicketCost(),
                    TicketTransaction.SourceType.RANDOM_BOX,
                    "Random Box purchase: " + box.getName());

            // 2. 확률 기반 상품 선택
            List<RandomBoxPrize> availablePrizes = box.getPrizes().stream()
                    .filter(RandomBoxPrize::isWinningAvailable)
                    .collect(Collectors.toList());

            RandomBoxPrize selectedPrize = selectPrize(availablePrizes);

            // 3. 포인트 지급 (포인트 상품인 경우)
            int pointsAwarded = 0;
            if (selectedPrize != null && selectedPrize.isBasicPointPrize() && selectedPrize.hasPointRange()) {
                pointsAwarded = calculateRandomPointValue(
                        selectedPrize.getMinPointValue(),
                        selectedPrize.getMaxPointValue());

                pointService.earnPoints(userId, pointsAwarded,
                        PointTransaction.SourceType.RANDOM_BOX,
                        String.valueOf(box.getId()),
                        "Random Box prize: " + selectedPrize.getPrizeName());
            }

            // 4. 당첨 이력 기록
            WinningHistory history = WinningHistory.builder()
                    .user(user)
                    .randomBox(box)
                    .randomBoxPrize(selectedPrize)
                    .pointsAwarded(pointsAwarded)
                    .ticketCost(box.getTicketCost())
                    .prizeName(selectedPrize != null ? selectedPrize.getPrizeName() : "No Prize")
                    .build();

            winningHistoryRepository.save(history);

            log.info("Box purchased - userId: {}, boxId: {}, prize: {}, points: {}",
                    userId, boxId,
                    selectedPrize != null ? selectedPrize.getPrizeName() : "none",
                    pointsAwarded);

            return RandomBoxPurchaseResult.builder()
                    .winningHistoryId(history.getId())
                    .prizeName(selectedPrize != null ? selectedPrize.getPrizeName() : null)
                    .pointsAwarded(pointsAwarded)
                    .ticketCost(box.getTicketCost())
                    .isWinner(selectedPrize != null)
                    .build();
        });
    }

    /**
     * 소셜 프루프: 최근 당첨 내역
     */
    @Transactional(readOnly = true)
    public List<WinningHistory> getRecentWinners() {
        return winningHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 포인트 범위 내 랜덤 값 생성
     */
    private int calculateRandomPointValue(int min, int max) {
        return min + secureRandom.nextInt(max - min + 1);
    }
}
