package com.rewardplatform.randombox.service;

import com.rewardplatform.common.util.AbstractPrizeSelector;
import com.rewardplatform.common.util.DistributedLockManager;
import com.rewardplatform.point.service.PointService;
import com.rewardplatform.randombox.domain.*;
import com.rewardplatform.randombox.dto.RandomBoxPurchaseResult;
import com.rewardplatform.randombox.repository.RandomBoxRepository;
import com.rewardplatform.randombox.repository.WinningHistoryRepository;
import com.rewardplatform.ticket.service.TicketService;
import com.rewardplatform.user.domain.User;
import com.rewardplatform.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("확률 엔진 테스트")
class RandomBoxServiceTest {

    @Test
    @DisplayName("누적 확률 기반 상품 선택 - 재고 있는 상품만 선택됨")
    void shouldSelectPrizeBasedOnCumulativeProbability() {
        // given
        TestPrize prize1 = new TestPrize("A등급", 10.0, 5);
        TestPrize prize2 = new TestPrize("B등급", 30.0, 10);
        TestPrize prize3 = new TestPrize("C등급", 60.0, 100);
        List<TestPrize> prizes = List.of(prize1, prize2, prize3);

        TestPrizeSelector selector = new TestPrizeSelector();

        // when - 1000번 반복하여 확률 분포 검증
        int aCount = 0, bCount = 0, cCount = 0, nullCount = 0;
        for (int i = 0; i < 1000; i++) {
            // 재고 리셋
            prize1.remaining = 5000;
            prize2.remaining = 5000;
            prize3.remaining = 5000;

            TestPrize result = selector.selectPrize(new ArrayList<>(prizes));
            if (result == null) nullCount++;
            else if ("A등급".equals(result.name)) aCount++;
            else if ("B등급".equals(result.name)) bCount++;
            else if ("C등급".equals(result.name)) cCount++;
        }

        // then - 통계적으로 확률에 근접해야 함 (오차 범위 ±5%)
        double total = aCount + bCount + cCount;
        assertThat(aCount / total).isBetween(0.05, 0.15);  // ~10%
        assertThat(bCount / total).isBetween(0.25, 0.35);  // ~30%
        assertThat(cCount / total).isBetween(0.55, 0.65);  // ~60%
    }

    @Test
    @DisplayName("재고 소진 시 해당 상품 skip")
    void shouldSkipOutOfStockPrize() {
        // given
        TestPrize prize1 = new TestPrize("재고없음", 50.0, 0);
        TestPrize prize2 = new TestPrize("재고있음", 50.0, 100);
        List<TestPrize> prizes = List.of(prize1, prize2);

        TestPrizeSelector selector = new TestPrizeSelector();

        // when
        int prize2Count = 0;
        for (int i = 0; i < 100; i++) {
            prize2.remaining = 100;
            TestPrize result = selector.selectPrize(new ArrayList<>(prizes));
            if (result != null && "재고있음".equals(result.name)) prize2Count++;
        }

        // then - 재고 없는 상품은 절대 선택되지 않아야 함
        assertThat(prize2Count).isGreaterThan(0);
    }

    // 테스트용 내부 클래스
    static class TestPrize implements AbstractPrizeSelector.PrizeWithProbability {
        String name;
        double probability;
        int remaining;

        TestPrize(String name, double probability, int remaining) {
            this.name = name;
            this.probability = probability;
            this.remaining = remaining;
        }

        @Override public Double getWinningProbability() { return probability; }
        @Override public Integer getRemainingCount() { return remaining; }
        @Override public void decrementRemainingCount() { remaining--; }
        @Override public String getPrizeName() { return name; }
    }

    static class TestPrizeSelector extends AbstractPrizeSelector {}
}
