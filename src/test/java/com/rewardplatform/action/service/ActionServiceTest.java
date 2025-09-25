package com.rewardplatform.action.service;

import com.rewardplatform.action.domain.Action;
import com.rewardplatform.action.domain.UserActionLog;
import com.rewardplatform.action.dto.ActionPlayRequest;
import com.rewardplatform.action.dto.ActionPlayResult;
import com.rewardplatform.action.repository.ActionRepository;
import com.rewardplatform.action.repository.UserActionLogRepository;
import com.rewardplatform.ticket.service.TicketService;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActionService 단위 테스트")
class ActionServiceTest {

    @InjectMocks
    private ActionService actionService;

    @Mock private ActionRepository actionRepository;
    @Mock private UserActionLogRepository actionLogRepository;
    @Mock private TicketService ticketService;
    @Mock private UserRepository userRepository;

    private Action rpsAction;
    private User testUser;

    @BeforeEach
    void setUp() {
        rpsAction = Action.builder()
                .id(1L)
                .actionName("Rock Paper Scissors")
                .actionType(Action.ActionType.RPS)
                .successReward(2)
                .failReward(1)
                .dailyLimit(10)
                .maxDailyReward(20)
                .isActive(true)
                .build();

        testUser = User.builder().id(1L).email("test@test.com").build();
    }

    @Nested
    @DisplayName("게임 플레이")
    class PlayAction {

        @Test
        @DisplayName("성공 시 성공 보상 지급")
        void shouldGiveSuccessReward() {
            // given
            given(actionRepository.findById(1L)).willReturn(Optional.of(rpsAction));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(actionLogRepository.countTodayParticipation(eq(1L), eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(actionLogRepository.sumTodayReward(eq(1L), eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(actionLogRepository.save(any(UserActionLog.class)))
                    .willReturn(UserActionLog.builder().build());

            // when
            ActionPlayResult result = actionService.playAction(1L,
                    new ActionPlayRequest(1L, true));

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getEarnedReward()).isEqualTo(2);
            assertThat(result.getRemainingAttempts()).isEqualTo(9);
            then(ticketService).should().earnTickets(eq(1L), eq(2), any(), any());
        }

        @Test
        @DisplayName("일일 참여 제한 초과 시 예외")
        void shouldThrowWhenDailyLimitReached() {
            // given
            given(actionRepository.findById(1L)).willReturn(Optional.of(rpsAction));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(actionLogRepository.countTodayParticipation(eq(1L), eq(1L), any(LocalDate.class)))
                    .willReturn(10); // 이미 10회 참여

            // when & then
            assertThatThrownBy(() ->
                    actionService.playAction(1L, new ActionPlayRequest(1L, true)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Daily limit reached");
        }

        @Test
        @DisplayName("실패해도 실패 보상 지급")
        void shouldGiveFailReward() {
            // given
            given(actionRepository.findById(1L)).willReturn(Optional.of(rpsAction));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(actionLogRepository.countTodayParticipation(eq(1L), eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(actionLogRepository.sumTodayReward(eq(1L), eq(1L), any(LocalDate.class)))
                    .willReturn(0);
            given(actionLogRepository.save(any(UserActionLog.class)))
                    .willReturn(UserActionLog.builder().build());

            // when
            ActionPlayResult result = actionService.playAction(1L,
                    new ActionPlayRequest(1L, false));

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getEarnedReward()).isEqualTo(1); // failReward
        }
    }
}
