package com.rewardplatform.invite.service;

import com.rewardplatform.invite.domain.*;
import com.rewardplatform.invite.dto.InviteStatusResponse;
import com.rewardplatform.invite.repository.*;
import com.rewardplatform.point.domain.PointTransaction;
import com.rewardplatform.point.service.PointService;
import com.rewardplatform.ticket.domain.TicketTransaction;
import com.rewardplatform.ticket.service.TicketService;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 친구 초대 서비스
 *
 * 초대 코드 기반 친구 추천 시스템.
 * - 초대자/피초대자 양방향 보상
 * - 5명 단위 마일스톤 추가 보상
 * - 마일스톤 체크는 별도 트랜잭션(REQUIRES_NEW)으로 격리
 *   → 마일스톤 실패 시 초대 프로세스 전체 롤백 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InviteService {

    private final InviteCodeRepository inviteCodeRepository;
    private final InviteHistoryRepository inviteHistoryRepository;
    private final InviteRewardRepository inviteRewardRepository;
    private final InviteMilestoneRewardRepository milestoneRewardRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final PointService pointService;

    private static final int INVITER_TICKET_REWARD = 10;
    private static final int INVITEE_TICKET_REWARD = 5;
    private static final int INVITER_POINT_REWARD = 100;
    private static final int INVITEE_POINT_REWARD = 50;

    /**
     * 초대 코드 조회/생성
     */
    @Transactional
    public String getOrCreateInviteCode(Long userId) {
        return inviteCodeRepository.findByUserId(userId)
                .map(InviteCode::getCode)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));

                    String code = generateUniqueCode();
                    InviteCode inviteCode = InviteCode.builder()
                            .code(code)
                            .user(user)
                            .build();
                    inviteCodeRepository.save(inviteCode);
                    return code;
                });
    }

    /**
     * 초대 코드 사용 (핵심 플로우)
     *
     * Flow:
     * 1. 코드 유효성 검증
     * 2. 중복 초대 방지
     * 3. 양방향 보상 지급
     * 4. 마일스톤 체크 (별도 트랜잭션)
     */
    @Transactional
    public void redeemInviteCode(Long inviteeId, String code) {
        // 중복 초대 방지
        if (inviteHistoryRepository.findByInviteeId(inviteeId).isPresent()) {
            throw new IllegalStateException("Already redeemed an invite code");
        }

        InviteCode inviteCode = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code: " + code));

        if (inviteCode.getUser().getId().equals(inviteeId)) {
            throw new IllegalStateException("Cannot use own invite code");
        }

        User inviter = inviteCode.getUser();
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 초대 이력 저장
        InviteHistory history = InviteHistory.builder()
                .inviter(inviter)
                .invitee(invitee)
                .build();
        inviteHistoryRepository.save(history);

        // 양방향 보상 지급
        distributeRewards(inviter.getId(), invitee.getId());

        // 마일스톤 체크 (별도 트랜잭션)
        try {
            checkMilestoneRewards(inviter.getId());
        } catch (Exception e) {
            log.error("Milestone reward check failed for inviter: {}", inviter.getId(), e);
            // 마일스톤 실패가 전체 초대를 롤백하지 않음
        }

        log.info("Invite redeemed - inviter: {}, invitee: {}", inviter.getId(), inviteeId);
    }

    /**
     * 초대 현황 조회
     */
    @Transactional(readOnly = true)
    public InviteStatusResponse getInviteStatus(Long userId) {
        String code = inviteCodeRepository.findByUserId(userId)
                .map(InviteCode::getCode)
                .orElse(null);

        int totalInvites = inviteHistoryRepository.countByInviterId(userId);

        return InviteStatusResponse.builder()
                .inviteCode(code)
                .totalInvites(totalInvites)
                .nextMilestone(calculateNextMilestone(totalInvites))
                .invitesUntilNextMilestone(calculateNextMilestone(totalInvites) - totalInvites)
                .build();
    }

    private void distributeRewards(Long inviterId, Long inviteeId) {
        // 초대자 보상
        ticketService.earnTickets(inviterId, INVITER_TICKET_REWARD,
                TicketTransaction.SourceType.INVITE, "Friend invite reward");
        pointService.earnPoints(inviterId, INVITER_POINT_REWARD,
                PointTransaction.SourceType.INVITE, null, "Friend invite reward");

        // 피초대자 보상
        ticketService.earnTickets(inviteeId, INVITEE_TICKET_REWARD,
                TicketTransaction.SourceType.INVITE, "Invited friend bonus");
        pointService.earnPoints(inviteeId, INVITEE_POINT_REWARD,
                PointTransaction.SourceType.INVITE, null, "Invited friend bonus");
    }

    /**
     * 마일스톤 보상 체크 (별도 트랜잭션으로 격리)
     *
     * REQUIRES_NEW: 마일스톤 실패가 메인 초대 트랜잭션에 영향을 주지 않도록 격리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkMilestoneRewards(Long inviterId) {
        int totalInvites = inviteHistoryRepository.countByInviterId(inviterId);

        List<InviteReward> milestones = inviteRewardRepository.findAllByOrderByMilestoneSortingAsc();

        for (InviteReward milestone : milestones) {
            if (totalInvites >= milestone.getMilestoneSorting()
                    && !milestoneRewardRepository.existsByUserIdAndInviteRewardId(inviterId, milestone.getId())) {

                // 마일스톤 달성 기록
                InviteMilestoneReward milestoneReward = InviteMilestoneReward.builder()
                        .user(userRepository.getReferenceById(inviterId))
                        .inviteReward(milestone)
                        .achievedDate(LocalDateTime.now())
                        .build();
                milestoneRewardRepository.save(milestoneReward);

                // 마일스톤 보상 지급
                if (milestone.getTicketReward() > 0) {
                    ticketService.earnTickets(inviterId, milestone.getTicketReward(),
                            TicketTransaction.SourceType.INVITE,
                            "Invite milestone: " + milestone.getMilestoneSorting() + " friends");
                }
                if (milestone.getPointReward() > 0) {
                    pointService.earnPoints(inviterId, milestone.getPointReward(),
                            PointTransaction.SourceType.INVITE, null,
                            "Invite milestone: " + milestone.getMilestoneSorting() + " friends");
                }

                log.info("Milestone achieved - userId: {}, milestone: {} invites",
                        inviterId, milestone.getMilestoneSorting());
            }
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (inviteCodeRepository.findByCode(code).isPresent());
        return code;
    }

    private int calculateNextMilestone(int currentInvites) {
        return ((currentInvites / 5) + 1) * 5;
    }
}
