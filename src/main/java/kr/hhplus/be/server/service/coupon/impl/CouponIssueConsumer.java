package kr.hhplus.be.server.service.coupon.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.lock.key.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 발급 Job 소비자
 * - 큐에서 꺼내 DB(UserCoupon) 저장
 * - 멱등 체크: 이미 발급된 경우 SKIP
 * - 실패 시 DLQ로 이동해 운영자가 처리할 수 있게 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final StringRedisTemplate redis;
    private final CouponJpaRepository couponRepo;
    private final UserCouponJpaRepository userCouponRepo;
    private final ObjectMapper om = new ObjectMapper();

    /** 간단한 폴링 소비자 (고빈도면 스레드/인스턴스 늘려 병렬 처리 가능) */
    @Scheduled(fixedDelay = 150, initialDelay = 1000)
    public void consume() {
        // 타임아웃 POP: 큐가 비어있으면 null 반환
        String payload = redis.opsForList().leftPop(RedisKeys.issueQueue(), Duration.ofSeconds(1));
        if (payload == null) return;

        try {
            IssueJob job = om.readValue(payload, IssueJob.class);
            processJob(job);
        } catch (Exception e) {
            log.error("Issue job failed, push to DLQ. payload={}", payload, e);
            redis.opsForList().rightPush(RedisKeys.issueDlq(), payload);
        }
    }

    @Transactional
    protected void processJob(IssueJob job) {
        Long couponId = job.couponId();
        Long userId   = job.userId();

        // 멱등: 동일 (userId, couponId)가 이미 DB에 있으면 스킵
        if (userCouponRepo.existsByUserIdAndCoupon_Id(userId, couponId)) {
            log.info("[SKIP] Duplicate persist: userId={}, couponId={}", userId, couponId);
            return;
        }

        Coupon coupon = couponRepo.findById(couponId)
                .orElseThrow(() -> new IllegalStateException("Coupon missing in DB: " + couponId));

        UserCoupon uc = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();

        userCouponRepo.save(uc);

        log.info("[OK] Persisted UserCoupon: userId={}, couponId={}, ucId={}", userId, couponId, uc.getId());
    }

    /** 큐 payload 직렬화 모델 */
    private record IssueJob(Long couponId, Long userId, Long issuedAt) {}
}
