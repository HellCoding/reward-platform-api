package com.rewardplatform.ticket.service;

import com.rewardplatform.common.event.InviteRewardEvent;
import com.rewardplatform.common.util.DistributedLockManager;
import com.rewardplatform.ticket.domain.Ticket;
import com.rewardplatform.ticket.domain.TicketTransaction;
import com.rewardplatform.ticket.domain.TicketTransaction.SourceType;
import com.rewardplatform.ticket.domain.TicketTransaction.TransactionType;
import com.rewardplatform.ticket.repository.TicketRepository;
import com.rewardplatform.ticket.repository.TicketTransactionRepository;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 티켓 관리 서비스
 *
 * 게임 액션의 보상으로 획득하고, 랜덤박스 구매에 사용하는 2차 화폐.
 * FIFO 기반 소진: 가장 오래된 EARN 트랜잭션부터 remainingAmount를 차감.
 *
 * 동시성 제어:
 * - Redis 분산 락으로 사용자별 동시 접근 방지
 * - Deadlock 감지 시 지수 백오프 재시도 (최대 3회)
 * - saveAndFlush()로 즉시 DB 반영하여 FIFO 정합성 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DistributedLockManager lockManager;

    private static final int MAX_DEADLOCK_RETRIES = 3;
    private static final long DEADLOCK_RETRY_BASE_MS = 200;

    /**
     * 티켓 적립 (Deadlock 재시도 포함)
     */
    @Transactional
    public TicketTransaction earnTickets(Long userId, int amount, SourceType sourceType, String description) {
        String lockKey = "ticket:user:" + userId;
        return lockManager.executeWithLock(lockKey, 5000, 10000, () ->
                executeWithDeadlockRetry(() -> earnTicketsInternal(userId, amount, sourceType, description))
        );
    }

    /**
     * 티켓 적립 내부 구현 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketTransaction earnTicketsInternal(Long userId, int amount, SourceType sourceType, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Ticket ticket = getOrCreateTicket(userId);
        ticket.earn(amount);

        TicketTransaction transaction = TicketTransaction.builder()
                .user(user)
                .amount(amount)
                .remainingAmount(amount)
                .transactionType(TransactionType.EARN)
                .sourceType(sourceType)
                .description(description)
                .build();

        transactionRepository.saveAndFlush(transaction);
        ticketRepository.saveAndFlush(ticket);

        log.info("Tickets earned - userId: {}, amount: {}, source: {}", userId, amount, sourceType);
        return transaction;
    }

    /**
     * 티켓 사용 (FIFO 기반 소진 + Deadlock 재시도)
     *
     * 가장 오래된 EARN 트랜잭션부터 remainingAmount를 차감합니다.
     * 부분 소진(partial consumption)을 지원하여 정확한 잔액 추적이 가능합니다.
     */
    @Transactional
    public TicketTransaction useTickets(Long userId, int amount, SourceType sourceType, String description) {
        String lockKey = "ticket:user:" + userId;
        return lockManager.executeWithLock(lockKey, 5000, 10000, () ->
                executeWithDeadlockRetry(() -> useTicketsInternal(userId, amount, sourceType, description))
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketTransaction useTicketsInternal(Long userId, int amount, SourceType sourceType, String description) {
        Ticket ticket = getOrCreateTicket(userId);
        if (ticket.getAvailableAmount() < amount) {
            throw new IllegalStateException("Insufficient tickets. Available: " + ticket.getAvailableAmount());
        }

        ticket.use(amount);

        // FIFO 소진: 가장 오래된 EARN부터 remainingAmount 차감
        int remaining = amount;
        List<TicketTransaction> earnTxList = transactionRepository.findAvailableEarnTransactions(userId);

        for (TicketTransaction earnTx : earnTxList) {
            if (remaining <= 0) break;

            int consume = Math.min(remaining, earnTx.getRemainingAmount());
            earnTx.consumePartially(consume);
            transactionRepository.saveAndFlush(earnTx);
            remaining -= consume;
        }

        if (remaining > 0) {
            log.warn("FIFO underflow - userId: {}, remaining: {}", userId, remaining);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        TicketTransaction useTx = TicketTransaction.builder()
                .user(user)
                .amount(amount)
                .transactionType(TransactionType.USE)
                .sourceType(sourceType)
                .description(description)
                .build();

        transactionRepository.saveAndFlush(useTx);
        ticketRepository.saveAndFlush(ticket);

        log.info("Tickets used - userId: {}, amount: {}, remaining: {}", userId, amount, ticket.getAvailableAmount());
        return useTx;
    }

    /**
     * Deadlock 감지 시 지수 백오프 재시도
     * 재시도 간격: 200ms → 400ms → 800ms
     */
    private <T> T executeWithDeadlockRetry(java.util.function.Supplier<T> task) {
        int retryCount = 0;
        while (true) {
            try {
                return task.get();
            } catch (CannotAcquireLockException e) {
                retryCount++;
                if (retryCount >= MAX_DEADLOCK_RETRIES) {
                    log.error("Deadlock retry exhausted after {} attempts", retryCount);
                    throw e;
                }
                long sleepMs = DEADLOCK_RETRY_BASE_MS * (1L << retryCount);
                log.warn("Deadlock detected, retrying ({}/{}), backoff: {}ms",
                        retryCount, MAX_DEADLOCK_RETRIES, sleepMs);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during deadlock retry", ie);
                }
            }
        }
    }

    /**
     * 친구 초대 이벤트 리스너
     * InviteService에서 발행한 이벤트를 수신하여 티켓 보상 지급
     */
    @EventListener
    public void handleInviteReward(InviteRewardEvent event) {
        try {
            earnTickets(event.getInviterId(), event.getInviterTicketReward(),
                    SourceType.INVITE, "Friend invite reward (event)");
            earnTickets(event.getInviteeId(), event.getInviteeTicketReward(),
                    SourceType.INVITE, "Invited friend bonus (event)");
        } catch (Exception e) {
            log.error("Failed to process invite ticket reward event", e);
        }
    }

    @Transactional(readOnly = true)
    public Ticket getTicketStatus(Long userId) {
        return getOrCreateTicket(userId);
    }

    private Ticket getOrCreateTicket(Long userId) {
        return ticketRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return ticketRepository.save(Ticket.builder().user(user).build());
                });
    }
}
