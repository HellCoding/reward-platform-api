package com.rewardplatform.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockManager 테스트")
class DistributedLockManagerTest {

    @InjectMocks
    private DistributedLockManager lockManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("락 획득 성공 시 작업 실행 후 락 해제")
    void shouldExecuteTaskAndReleaseLock() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);
        AtomicInteger counter = new AtomicInteger(0);

        // when
        Integer result = lockManager.executeWithLock("test-key", 5000, 30000,
                () -> counter.incrementAndGet());

        // then
        assertThat(result).isEqualTo(1);
        // releaseLock은 UUID 매칭 후 delete - get이 호출됨을 검증
        then(valueOperations).should(atLeastOnce()).get(anyString());
    }

    @Test
    @DisplayName("락 획득 실패 시 예외 발생")
    void shouldThrowWhenLockAcquisitionFails() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                lockManager.executeWithLock("test-key", 100, 30000, () -> "result"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to acquire lock");
    }

    @Test
    @DisplayName("리더 선출 성공")
    void shouldElectLeader() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        boolean isLeader = lockManager.tryBecomeLeader("pointExpiry", 60000);

        // then
        assertThat(isLeader).isTrue();
    }
}
