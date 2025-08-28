package kr.hhplus.be.server.service.coupon.impl;

import kr.hhplus.be.server.controller.coupon.response.IssueCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.lock.key.RedisKeys;
import kr.hhplus.be.server.service.coupon.RedisCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponServiceImpl implements RedisCouponService {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> fcfsIssueScript;
    private final CouponJpaRepository couponRepo;


    public IssueCouponResponse issueAsync(Long couponId, Long userId) {

        ensureCouponLoaded(couponId);

        String couponKey = RedisKeys.couponHash(couponId);
        String issuedKey = RedisKeys.issuedSet(couponId);
        String queueKey  = RedisKeys.issueQueue();

        long nowEpochSec = Instant.now().getEpochSecond();

        Long result = redis.execute(
                fcfsIssueScript,
                List.of(couponKey, issuedKey, queueKey),
                String.valueOf(userId),
                String.valueOf(nowEpochSec),
                String.valueOf(couponId)
        );

        if (result == null) {
            log.warn("Lua returned null (unexpected)");
            return IssueCouponResponse.notFound(couponId, userId);
        }

        if (result >= 0) {
            return IssueCouponResponse.accepted(couponId, userId, result);
        }

        return switch (result.intValue()) {
            case -1 -> IssueCouponResponse.soldOut(couponId, userId);
            case -2 -> IssueCouponResponse.duplicate(couponId, userId);
            case -3 -> IssueCouponResponse.notActive(couponId, userId);
            case -4 -> IssueCouponResponse.notFound(couponId, userId);
            default -> {
                log.warn("Unknown lua code: {}", result);
                yield IssueCouponResponse.notFound(couponId, userId);
            }
        };
    }


    @Transactional(readOnly = true)
    public void ensureCouponLoaded(Long couponId) {
        String couponKey = RedisKeys.couponHash(couponId);
        Boolean exists = redis.hasKey(couponKey);
        if (Boolean.TRUE.equals(exists)) return;

        Coupon c = couponRepo.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다. id=" + couponId));

        ZoneId zone = ZoneId.systemDefault(); // 테스트/운영 동일 기준
        long vfrom = c.getValidFrom().atZone(zone).toEpochSecond();
        long vto   = c.getValidTo().atZone(zone).toEpochSecond();

        // Redis Hash에 메타/재고 저장
        redis.opsForHash().put(couponKey, "id", String.valueOf(c.getId()));
        redis.opsForHash().put(couponKey, "name", c.getName());
        redis.opsForHash().put(couponKey, "discount_amount", String.valueOf(c.getDiscountAmount()));
        redis.opsForHash().put(couponKey, "stock", String.valueOf(c.getQuantity())); // 초기 재고 = DB quantity
        redis.opsForHash().put(couponKey, "valid_from_ts", String.valueOf(vfrom));
        redis.opsForHash().put(couponKey, "valid_to_ts", String.valueOf(vto));

        long now = Instant.now().getEpochSecond();
        long ttlSec = Math.max(1, vto - now);
        redis.expire(couponKey, java.time.Duration.ofSeconds(ttlSec));

        // issued Set도 같은 TTL로 맞춰서 기간 종료 시 자동 정리
        String issuedKey = RedisKeys.issuedSet(couponId);
        redis.expire(issuedKey, java.time.Duration.ofSeconds(ttlSec));
    }
}
