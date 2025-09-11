package kr.hhplus.be.server.service.coupon.impl;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.infrastructure.messaging.kafka.coupon.KafkaCouponIssueDTO;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponJpaRepository couponRepository;
    private final UserCouponJpaRepository userCouponRepository;

    @Qualifier("couponIssueKafkaTemplate")
    private final KafkaTemplate<Long, KafkaCouponIssueDTO> couponIssueTemplate;

    /* 분산락 사용 쿠폰 발급 */
/*    @Override
    @DistributedLock(
            keys = {
                    "'user:' + #userId",
                    "'coupon:' + #couponId"
            },
            prefix = "lock:",
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 2,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 100
    )
    @Transactional
    public boolean issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .filter(Coupon::isActive)
                .orElse(null);
        if (coupon == null || coupon.getQuantity() <= 0) return false;

        // 락을 건 후 직접 조회해서 존재 여부 확인
        Optional<UserCoupon> existing = userCouponRepository.findByUserIdAndCouponIdWithLock(userId, couponId);
        if (existing.isPresent()) return false;

        coupon.decreaseQuantity();
        couponRepository.save(coupon);

        try {
            userCouponRepository.save(UserCoupon.builder()
                    .userId(userId)
                    .coupon(coupon)
                    .build());
        } catch (DataIntegrityViolationException e) {
            rollback(userId, couponId);
            return false;
        }

        return true;
    }*/

    /** 카프카 사용 선착순 쿠폰 발급 */
    @Override
    public boolean issueCoupon(Long userId, Long couponId) {
        KafkaCouponIssueDTO payload = new KafkaCouponIssueDTO(
                userId,
                couponId,
                System.currentTimeMillis()
        );

        CompletableFuture<SendResult<Long, KafkaCouponIssueDTO>> future =
                couponIssueTemplate.send(KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, payload);

        future.whenComplete((SendResult<Long, KafkaCouponIssueDTO> result, Throwable ex) -> {
            if (ex == null) {
                RecordMetadata meta = result.getRecordMetadata();
                log.info("[coupon-issue] published: user={} coupon={} meta=topic:{} partition:{} offset:{}",
                        userId, couponId, meta.topic(), meta.partition(), meta.offset());
            } else {
                log.error("[coupon-issue] publish failed: user={} coupon={}", userId, couponId, ex);
            }
        });

        return true;
    }

    @Override
    public List<OwnedCouponResponse> getOwnedCoupons(Long userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
                .map(uc -> OwnedCouponResponse.from(uc.getCoupon()))
                .toList();
    }

    @Override
    public List<RegisteredCouponResponse> getRegisteredCoupons() {
        return couponRepository.findAll().stream()
                .map(coupon -> RegisteredCouponResponse.from(coupon, coupon.getQuantity()))
                .toList();
    }

    @Override
    @DistributedLock(
            prefix = "lock:coupon",
            keys = { "#userId + ':' + #couponId" },
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 2,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 100
    )
    @Transactional
    public boolean useCoupon(Long userId, Long couponId) {
        Optional<UserCoupon> optional = userCouponRepository.findByUserIdAndCouponIdWithLock(userId, couponId);
        if (optional.isEmpty()) return false;

        UserCoupon userCoupon = optional.get();
        if (userCoupon.isUsed()) return false;

        userCoupon.markUsed();
        userCouponRepository.save(userCoupon);
        return true;
    }

    @Override
    public int applyCoupon(Long userId, Long couponId) {
        return userCouponRepository.findByUserIdAndCoupon_Id(userId, couponId)
                .filter(uc -> !uc.isUsed() && uc.getCoupon().isActive())
                .map(uc -> uc.getCoupon().getDiscountAmount())
                .orElse(0);
    }

    @Override
    @DistributedLock(
            prefix = "lock:coupon",
            keys = { "#userId + ':' + #couponId" },
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 2,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 100
    )
    @Transactional
    public void rollback(Long userId, Long couponId) {
        userCouponRepository.findByUserIdAndCouponIdWithLock(userId, couponId)
                .ifPresent(uc -> {
                    uc.rollback();
                    userCouponRepository.save(uc);

                    Coupon coupon = uc.getCoupon();
                    coupon.increaseQuantity();
                    couponRepository.save(coupon);
                });
    }
}

