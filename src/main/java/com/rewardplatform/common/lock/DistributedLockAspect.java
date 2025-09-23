package com.rewardplatform.common.lock;

import com.rewardplatform.common.util.DistributedLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @DistributedLock AOP Aspect
 *
 * 어노테이션이 적용된 메서드 실행 시 자동으로 Redis 분산 락을 획득/해제합니다.
 * SpEL 표현식으로 동적 락 키를 지원합니다.
 *
 * 예: @DistributedLock(name = "point", key = "#userId")
 * → 락 키: "point:123" (userId가 123인 경우)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final DistributedLockManager lockManager;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = resolveLockKey(joinPoint, distributedLock);

        return lockManager.executeWithLock(
                lockKey,
                distributedLock.waitMs(),
                distributedLock.leaseMs(),
                () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private String resolveLockKey(ProceedingJoinPoint joinPoint, DistributedLock lock) {
        String key = lock.key();
        if (key.isEmpty()) {
            return lock.name();
        }

        // SpEL로 동적 키 해석
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Object resolved = parser.parseExpression(key).getValue(context);
        return lock.name() + ":" + resolved;
    }
}
