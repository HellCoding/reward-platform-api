package com.rewardplatform.common.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;

/**
 * 확률 기반 상품 선택 엔진 (추상 클래스)
 *
 * SecureRandom + BigDecimal 정밀 연산으로 공정한 확률 보장.
 * 누적 확률 방식: 각 상품의 확률을 순차 합산하여 랜덤값이 해당 구간에 속하면 당첨.
 *
 * 예: [A=30%, B=50%, C=20%]
 *   → 랜덤값 0~30 → A, 30~80 → B, 80~100 → C
 *
 * 재고 관리: remainingCount > 0인 상품만 선택 가능
 */
@Slf4j
public abstract class AbstractPrizeSelector {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 확률 기반 상품 선택
     *
     * @param prizes 확률이 설정된 상품 목록
     * @return 선택된 상품 (재고 소진 시 null)
     */
    public <T extends PrizeWithProbability> T selectPrize(List<T> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            return null;
        }

        BigDecimal totalProbability = prizes.stream()
                .map(p -> BigDecimal.valueOf(p.getWinningProbability()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal randomValue = generateRandomValue(totalProbability);
        BigDecimal cumulative = BigDecimal.ZERO;

        log.debug("Prize selection - total: {}, random: {}", totalProbability, randomValue);

        for (T prize : prizes) {
            cumulative = cumulative.add(BigDecimal.valueOf(prize.getWinningProbability()));

            if (randomValue.compareTo(cumulative) <= 0) {
                if (prize.getRemainingCount() != null && prize.getRemainingCount() > 0) {
                    prize.decrementRemainingCount();
                    log.info("Prize selected: {}, remaining: {}", prize.getPrizeName(), prize.getRemainingCount());
                    return prize;
                }
                log.debug("Prize {} selected but out of stock", prize.getPrizeName());
            }
        }

        return null;
    }

    /**
     * 0 ~ totalProbability 범위의 랜덤 값 생성
     */
    protected BigDecimal generateRandomValue(BigDecimal totalProbability) {
        double random = SECURE_RANDOM.nextDouble();
        return totalProbability.multiply(BigDecimal.valueOf(random))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 확률이 설정된 상품 인터페이스
     */
    public interface PrizeWithProbability {
        Double getWinningProbability();
        Integer getRemainingCount();
        void decrementRemainingCount();
        String getPrizeName();
    }
}
