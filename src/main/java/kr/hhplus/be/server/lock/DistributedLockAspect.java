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
    public Object lock(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        DistributedLock ann = method.getAnnotation(DistributedLock.class);

        List<String> keys = resolveKeys(pjp, ann);
        if (keys.isEmpty()) throw new IllegalStateException("분산락 키가 없습니다.");

        List<RLock> locks = keys.stream().map(redissonClient::getLock).toList();
        RedissonMultiLock multiLock = new RedissonMultiLock(locks.toArray(new RLock[0]));

        Duration wait = Duration.of(ann.waitFor(), ann.waitUnit());
        Duration lease = Duration.of(ann.lease(), ann.unit());

        boolean acquired = multiLock.tryLock(
                wait.toMillis(),
                lease.isZero() ? -1 : lease.toMillis(), // 0이면 watchdog(-1)로 유지
                TimeUnit.MILLISECONDS
        );
        if (!acquired) throw new IllegalStateException("LOCK_TIMEOUT: " + keys);

        log.debug("LOCK ACQUIRED: {}", keys);

        boolean willUnlockByTx = false;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        safeUnlock(multiLock, keys, "afterCompletion");
                    }
                });
                willUnlockByTx = true;
            } catch (IllegalStateException e) {
                log.warn("Tx sync registration failed, fallback to finally unlock. msg={}", e.getMessage());
            }
        }

        try {
            return pjp.proceed();
        } finally {
            if (!willUnlockByTx) {
                safeUnlock(multiLock, keys, "finally");
            }
        }
    }

    private void safeUnlock(RedissonMultiLock multiLock, List<String> keys, String where) {
        try {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
                log.debug("LOCK RELEASED({}): {}", where, keys);
            }
        } catch (Exception e) {
            log.error("UNLOCK FAILED({}): {} - {}", where, keys, e.getMessage(), e);
        }
    }

    private List<String> resolveKeys(ProceedingJoinPoint pjp, DistributedLock ann) {
        String[] exprs = ann.keys();
        if (exprs.length == 0) return List.of();

        EvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        String[] paramNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        return Arrays.stream(exprs)
                .map(e -> parser.parseExpression(e).getValue(ctx, String.class))
                .filter(Objects::nonNull)
                .map(val -> ann.prefix() + ":" + val)
                .toList();
    }
}
