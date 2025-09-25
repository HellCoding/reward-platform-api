package com.rewardplatform.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderElectionService 테스트")
class LeaderElectionServiceTest {

    @InjectMocks
    private LeaderElectionService leaderElectionService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("리더 선출 성공 - SET NX 성공")
    void shouldElectLeaderOnSetNxSuccess() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        boolean result = leaderElectionService.tryBecomeLeader("point-expiration", 300000);

        // then
        assertThat(result).isTrue();
        assertThat(leaderElectionService.isLeader()).isTrue();
    }

    @Test
    @DisplayName("리더 선출 실패 - 다른 인스턴스가 리더")
    void shouldNotBeLeaderWhenOtherInstanceIsLeader() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false);
        given(valueOperations.get(anyString())).willReturn("other-instance-id");

        // when
        boolean result = leaderElectionService.tryBecomeLeader("point-expiration", 300000);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Redis 장애 시 fallback으로 리더 선출")
    void shouldFallbackWhenRedisUnavailable() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willThrow(new RuntimeException("Redis connection refused"));

        // when
        boolean result = leaderElectionService.tryBecomeLeader("point-expiration", 300000);

        // then
        assertThat(result).isTrue(); // fallback: 로컬 환경이면 항상 리더
    }
}
