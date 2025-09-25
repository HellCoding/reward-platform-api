package com.rewardplatform.invite.service;

import com.rewardplatform.invite.domain.*;
import com.rewardplatform.invite.repository.*;
import com.rewardplatform.point.service.PointService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InviteService 단위 테스트")
class InviteServiceTest {

    @InjectMocks
    private InviteService inviteService;

    @Mock private InviteCodeRepository inviteCodeRepository;
    @Mock private InviteHistoryRepository inviteHistoryRepository;
    @Mock private InviteRewardRepository inviteRewardRepository;
    @Mock private InviteMilestoneRewardRepository milestoneRewardRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketService ticketService;
    @Mock private PointService pointService;

    private User inviter;
    private User invitee;

    @BeforeEach
    void setUp() {
        inviter = User.builder().id(1L).email("inviter@test.com").name("초대자").build();
        invitee = User.builder().id(2L).email("invitee@test.com").name("피초대자").build();
    }

    @Nested
    @DisplayName("초대 코드 사용")
    class RedeemInviteCode {

        @Test
        @DisplayName("이미 초대받은 사용자는 재사용 불가")
        void shouldRejectDuplicateInvitee() {
            // given
            given(inviteHistoryRepository.findByInviteeId(2L))
                    .willReturn(Optional.of(InviteHistory.builder().build()));

            // when & then
            assertThatThrownBy(() -> inviteService.redeemInviteCode(2L, "ABCD1234"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already redeemed");
        }

        @Test
        @DisplayName("자기 자신의 초대 코드 사용 불가")
        void shouldRejectSelfInvite() {
            // given
            given(inviteHistoryRepository.findByInviteeId(1L))
                    .willReturn(Optional.empty());
            given(inviteCodeRepository.findByCode("ABCD1234"))
                    .willReturn(Optional.of(InviteCode.builder()
                            .code("ABCD1234")
                            .user(inviter)
                            .build()));

            // when & then
            assertThatThrownBy(() -> inviteService.redeemInviteCode(1L, "ABCD1234"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("own invite code");
        }

        @Test
        @DisplayName("성공 시 양방향 보상 지급")
        void shouldDistributeBidirectionalRewards() {
            // given
            given(inviteHistoryRepository.findByInviteeId(2L)).willReturn(Optional.empty());
            given(inviteCodeRepository.findByCode("ABCD1234"))
                    .willReturn(Optional.of(InviteCode.builder()
                            .code("ABCD1234")
                            .user(inviter)
                            .build()));
            given(userRepository.findById(2L)).willReturn(Optional.of(invitee));
            given(inviteHistoryRepository.save(any())).willReturn(InviteHistory.builder().build());
            given(inviteHistoryRepository.countByInviterId(1L)).willReturn(1);
            given(inviteRewardRepository.findAllByOrderByMilestoneSortingAsc())
                    .willReturn(java.util.List.of());

            // when
            inviteService.redeemInviteCode(2L, "ABCD1234");

            // then - 초대자 보상
            then(ticketService).should().earnTickets(eq(1L), eq(10), any(), any());
            then(pointService).should().earnPoints(eq(1L), eq(100), any(), any(), any());

            // then - 피초대자 보상
            then(ticketService).should().earnTickets(eq(2L), eq(5), any(), any());
            then(pointService).should().earnPoints(eq(2L), eq(50), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("초대 현황")
    class InviteStatus {

        @Test
        @DisplayName("다음 마일스톤 계산")
        void shouldCalculateNextMilestone() {
            // given
            given(inviteCodeRepository.findByUserId(1L))
                    .willReturn(Optional.of(InviteCode.builder().code("ABCD1234").user(inviter).build()));
            given(inviteHistoryRepository.countByInviterId(1L)).willReturn(7);

            // when
            var status = inviteService.getInviteStatus(1L);

            // then
            assertThat(status.getTotalInvites()).isEqualTo(7);
            assertThat(status.getNextMilestone()).isEqualTo(10); // 다음: 10명
            assertThat(status.getInvitesUntilNextMilestone()).isEqualTo(3); // 3명 남음
        }
    }
}
