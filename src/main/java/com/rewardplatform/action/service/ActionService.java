package com.rewardplatform.action.service;

import com.rewardplatform.action.domain.Action;
import com.rewardplatform.action.domain.UserActionLog;
import com.rewardplatform.action.dto.ActionPlayRequest;
import com.rewardplatform.action.dto.ActionPlayResult;
import com.rewardplatform.action.dto.AvailableActionResponse;
import com.rewardplatform.action.repository.ActionRepository;
import com.rewardplatform.action.repository.UserActionLogRepository;
import com.rewardplatform.ticket.domain.TicketTransaction;
import com.rewardplatform.ticket.service.TicketService;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게임 액션 서비스
 *
 * 8종 이상의 미니게임(출석체크, 가위바위보, 룰렛 등) 비즈니스 로직.
 * 일일 참여 제한, 보상 상한, 이벤트 배율을 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {

    private final ActionRepository actionRepository;
    private final UserActionLogRepository actionLogRepository;
    private final TicketService ticketService;
    private final UserRepository userRepository;

    /**
     * 참여 가능한 게임 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AvailableActionResponse> getAvailableActions(Long userId) {
        LocalDate today = LocalDate.now();

        return actionRepository.findByIsActiveTrueOrderByOrdAsc().stream()
                .map(action -> {
                    int todayCount = actionLogRepository.countTodayParticipation(
                            userId, action.getId(), today);
                    int todayReward = actionLogRepository.sumTodayReward(
                            userId, action.getId(), today);

                    return AvailableActionResponse.builder()
                            .actionId(action.getId())
                            .actionName(action.getActionName())
                            .actionType(action.getActionType().name())
                            .successReward(action.getSuccessReward())
                            .failReward(action.getFailReward())
                            .dailyLimit(action.getDailyLimit())
                            .remainingAttempts(Math.max(0, action.getDailyLimit() - todayCount))
                            .todayEarnedReward(todayReward)
                            .maxDailyReward(action.getMaxDailyReward())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 게임 액션 수행
     *
     * Flow:
     * 1. 일일 참여 횟수 확인
     * 2. 일일 보상 상한 확인
     * 3. 게임 결과에 따른 보상 결정
     * 4. 티켓 지급
     * 5. 이력 기록
     */
    @Transactional
    public ActionPlayResult playAction(Long userId, ActionPlayRequest request) {
        Action action = actionRepository.findById(request.getActionId())
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + request.getActionId()));

        if (!action.isActive()) {
            throw new IllegalStateException("Action is not active");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();

        // 일일 참여 제한 확인
        int todayCount = actionLogRepository.countTodayParticipation(userId, action.getId(), today);
        if (todayCount >= action.getDailyLimit()) {
            throw new IllegalStateException("Daily limit reached for action: " + action.getActionName());
        }

        // 일일 보상 상한 확인
        int todayReward = actionLogRepository.sumTodayReward(userId, action.getId(), today);
        if (todayReward >= action.getMaxDailyReward()) {
            throw new IllegalStateException("Max daily reward reached for action: " + action.getActionName());
        }

        // 보상 결정
        boolean success = request.isSuccess();
        int reward = success ? action.getSuccessReward() : action.getFailReward();

        // 보상 상한 초과 방지
        reward = Math.min(reward, action.getMaxDailyReward() - todayReward);

        // 티켓 지급
        if (reward > 0) {
            ticketService.earnTickets(userId, reward,
                    TicketTransaction.SourceType.ACTION,
                    action.getActionName() + " - " + (success ? "SUCCESS" : "FAIL"));
        }

        // 이력 기록
        UserActionLog actionLog = UserActionLog.builder()
                .user(user)
                .action(action)
                .success(success)
                .earnedReward(reward)
                .participationDate(today)
                .build();

        actionLogRepository.save(actionLog);

        log.info("Action played - userId: {}, action: {}, success: {}, reward: {}",
                userId, action.getActionType(), success, reward);

        return ActionPlayResult.builder()
                .actionType(action.getActionType().name())
                .success(success)
                .earnedReward(reward)
                .remainingAttempts(action.getDailyLimit() - todayCount - 1)
                .build();
    }
}
