package kr.hhplus.be.server.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(kr.hhplus.be.server.lock.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        List<String> resolvedKeys = resolveKeys(joinPoint, annotation);

        if (resolvedKeys.isEmpty()) {
            throw new IllegalStateException("분산락 키가 없습니다.");
        }

        List<RLock> locks = resolvedKeys.stream()
                .map(key -> redissonClient.getLock(key))
                .toList();

        RedissonMultiLock multiLock = new RedissonMultiLock(locks.toArray(new RLock[0]));

        Duration waitTime = Duration.of(annotation.waitFor(), annotation.waitUnit());
        Duration leaseTime = Duration.of(annotation.lease(), annotation.unit());

        boolean locked = multiLock.tryLock(
                waitTime.toMillis(),
                leaseTime.isZero() ? -1 : leaseTime.toMillis(),
                TimeUnit.MILLISECONDS
        );

        if (!locked) {
            throw new IllegalStateException("LOCK_TIMEOUT: " + resolvedKeys);
        }

        try {
            log.debug("LOCK ACQUIRED: {}", resolvedKeys);
            return joinPoint.proceed();
        } finally {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (multiLock.isHeldByCurrentThread()) {
                        try {
                            multiLock.unlock();
                            log.debug("LOCK RELEASED: {}", resolvedKeys);
                        } catch (Exception e) {
                            log.error("UNLOCK FAILED: {}", e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private List<String> resolveKeys(ProceedingJoinPoint joinPoint, DistributedLock annotation) {
        String[] expressions = annotation.keys();
        if (expressions.length == 0) return List.of();

        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return Arrays.stream(expressions)
                .map(expr -> annotation.prefix() + ":" + parser.parseExpression(expr).getValue(context, String.class))
                .filter(Objects::nonNull)
                .toList();
    }
}
