package com.rewardplatform.point.service;

import com.rewardplatform.common.util.DistributedLockManager;
import com.rewardplatform.point.domain.Point;
import com.rewardplatform.point.domain.PointTransaction;
import com.rewardplatform.point.repository.PointRepository;
import com.rewardplatform.point.repository.PointTransactionRepository;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 단위 테스트")
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DistributedLockManager lockManager;

    private User testUser;
    private Point testPoint;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트 유저")
                .build();

        testPoint = Point.builder()
                .id(1L)
                .user(testUser)
                .totalAmount(0)
                .availableAmount(0)
                .build();

        // @Value 필드 주입
        Field expirationField = PointService.class.getDeclaredField("defaultExpirationDays");
        expirationField.setAccessible(true);
        expirationField.set(pointService, 365);
    }

    @Nested
    @DisplayName("가용 포인트 계산")
    class CalculateAvailablePoints {

        @Test
        @DisplayName("EARN - USE - EXPIRE = 가용 포인트")
        void shouldCalculateFromLedger() {
            // given
            given(transactionRepository.sumActiveEarnedPoints(eq(1L), any(LocalDateTime.class)))
                    .willReturn(1000);
            given(transactionRepository.sumUsedPoints(1L)).willReturn(300);
            given(transactionRepository.sumExpiredPoints(1L)).willReturn(200);

            // when
            int available = pointService.calculateAvailablePoints(1L);

            // then
            assertThat(available).isEqualTo(500); // 1000 - 300 - 200
        }

        @Test
        @DisplayName("음수인 경우 0 반환")
        void shouldReturnZeroWhenNegative() {
            // given
            given(transactionRepository.sumActiveEarnedPoints(eq(1L), any(LocalDateTime.class)))
                    .willReturn(100);
            given(transactionRepository.sumUsedPoints(1L)).willReturn(200);
            given(transactionRepository.sumExpiredPoints(1L)).willReturn(0);

            // when
            int available = pointService.calculateAvailablePoints(1L);

            // then
            assertThat(available).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("포인트 적립")
    class EarnPoints {

        @Test
        @DisplayName("성공 시 트랜잭션 생성 및 잔액 증가")
        void shouldEarnPointsSuccessfully() {
            // given
            given(lockManager.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                    .willAnswer(invocation -> {
                        Supplier<?> task = invocation.getArgument(3);
                        return task.get();
                    });
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(pointRepository.findByUserId(1L)).willReturn(Optional.of(testPoint));
            given(transactionRepository.save(any(PointTransaction.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(pointRepository.save(any(Point.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            PointTransaction result = pointService.earnPoints(
                    1L, 100, PointTransaction.SourceType.RANDOM_BOX, "box-1", "Random box reward");

            // then
            assertThat(result.getAmount()).isEqualTo(100);
            assertThat(result.getTransactionType()).isEqualTo(PointTransaction.TransactionType.EARN);
            assertThat(result.getSourceType()).isEqualTo(PointTransaction.SourceType.RANDOM_BOX);
            assertThat(result.getExpirationDate()).isAfter(LocalDateTime.now().plusDays(364));
            assertThat(testPoint.getTotalAmount()).isEqualTo(100);
            assertThat(testPoint.getAvailableAmount()).isEqualTo(100);

            then(transactionRepository).should().save(any(PointTransaction.class));
            then(pointRepository).should().save(testPoint);
        }
    }

    @Nested
    @DisplayName("포인트 사용")
    class UsePoints {

        @Test
        @DisplayName("잔액 부족 시 예외 발생")
        void shouldThrowWhenInsufficientPoints() {
            // given
            given(lockManager.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                    .willAnswer(invocation -> {
                        Supplier<?> task = invocation.getArgument(3);
                        return task.get();
                    });
            given(transactionRepository.sumActiveEarnedPoints(eq(1L), any(LocalDateTime.class)))
                    .willReturn(50);
            given(transactionRepository.sumUsedPoints(1L)).willReturn(0);
            given(transactionRepository.sumExpiredPoints(1L)).willReturn(0);

            // when & then
            assertThatThrownBy(() ->
                    pointService.usePoints(1L, 100, PointTransaction.SourceType.RANDOM_BOX, "box-1", "Purchase"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient points");
        }
    }
}
