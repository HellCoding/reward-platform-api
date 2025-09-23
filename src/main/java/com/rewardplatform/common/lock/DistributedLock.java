package com.rewardplatform.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산 락 어노테이션
 *
 * 메서드 레벨에 적용하면 AOP Aspect가 자동으로 Redis 분산 락을 관리합니다.
 *
 * <pre>
 * {@code
 * @DistributedLock(name = "point-earn", key = "#userId", leaseMs = 30000)
 * public void earnPoints(Long userId, int amount) {
 *     // 분산 락이 자동으로 획득/해제됩니다.
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** 락 이름 (prefix) */
    String name();

    /** 동적 키 (SpEL 지원, e.g. "#userId") */
    String key() default "";

    /** 락 획득 대기 시간 (ms) */
    long waitMs() default 5000;

    /** 락 유지 시간 (ms) */
    long leaseMs() default 30000;
}
