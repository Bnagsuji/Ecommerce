package kr.hhplus.be.server.service.coupon.impl;

import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.service.coupon.CouponService;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CouponServiceImpl implements CouponService {
    //테스트에서 사용할 getter
    // 쿠폰 정보 저장 (id → Coupon)
    @Getter
    private final Map<Long, Coupon> couponMap = new ConcurrentHashMap<>();

    // 쿠폰 수량 저장 (id → 남은 수량)
    @Getter
    private final Map<Long, Integer> couponQuantities = new ConcurrentHashMap<>();

    // 유저가 소유한 쿠폰 목록 (userId → [couponId])
    private final Map<Long, Set<Long>> userCoupons = new ConcurrentHashMap<>();

    // 유저가 사용한 쿠폰 목록 (userId → [couponId])
    private final Map<Long, Set<Long>> usedCoupons = new ConcurrentHashMap<>();

    // 쿠폰별 할인 금액
    @Getter
    private final Map<Long, Integer> couponDiscounts = Map.of(
            100L, 1000,
            200L, 2000
    );

    public CouponServiceImpl() {
        // 예시 쿠폰 초기 등록
        couponMap.put(100L, new Coupon(100L, "할인쿠폰1000", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)));
        couponMap.put(200L, new Coupon(200L, "할인쿠폰2000", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7)));

        couponQuantities.put(100L, 3);  // 선착순 3명
        couponQuantities.put(200L, 5);  // 선착순 5명
    }

    @Override
    public boolean issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMap.get(couponId);
        if (coupon == null || !coupon.isActive()) return false;

        synchronized (couponQuantities) {
            // 이미 보유한 쿠폰인지 확인
            userCoupons.putIfAbsent(userId, new HashSet<>());
            if (userCoupons.get(userId).contains(couponId)) return false;

            // 남은 수량 확인
            int remain = couponQuantities.getOrDefault(couponId, 0);
            if (remain <= 0) return false;

            // 발급
            userCoupons.get(userId).add(couponId);
            couponQuantities.put(couponId, remain - 1);

            return true;
        }
    }

    @Override
    public List<OwnedCouponResponse> getOwnedCoupons(Long userId) {
        Set<Long> owned = userCoupons.getOrDefault(userId, Collections.emptySet());
        List<OwnedCouponResponse> result = new ArrayList<>();

        for (Long couponId : owned) {
            Coupon c = couponMap.get(couponId);
            if (c == null) continue;
            boolean expired = LocalDateTime.now().isAfter(c.getValidTo());
            result.add(new OwnedCouponResponse(couponId, c.getName(), expired));
        }
        return result;
    }

    @Override
    public List<RegisteredCouponResponse> getRegisteredCoupons() {
        List<RegisteredCouponResponse> result = new ArrayList<>();
        for (Coupon c : couponMap.values()) {
            result.add(new RegisteredCouponResponse(
                    c.getId(),
                    c.getName(),
                    c.isActive(),
                    couponQuantities.getOrDefault(c.getId(), 0)
            ));
        }
        return result;
    }

    @Override
    public boolean useCoupon(Long userId, Long couponId) {
        if (!userCoupons.getOrDefault(userId, Set.of()).contains(couponId)) return false;

        usedCoupons.putIfAbsent(userId, new HashSet<>());
        if (usedCoupons.get(userId).contains(couponId)) return false;

        usedCoupons.get(userId).add(couponId);
        return true;
    }

    @Override
    public int applyCoupon(Long userId, Long couponId) {
        return couponDiscounts.getOrDefault(couponId, 0);
    }

    @Override
    public void rollback(Long userId, Long couponId) {
        Set<Long> used = usedCoupons.getOrDefault(userId, Collections.emptySet());
        used.remove(couponId);
    }


    public Map<Long, Set<Long>> getUserCoupons() {
        return userCoupons;
    }

    public Map<Long, Set<Long>> getUsedCoupons() {
        return usedCoupons;
    }

}
