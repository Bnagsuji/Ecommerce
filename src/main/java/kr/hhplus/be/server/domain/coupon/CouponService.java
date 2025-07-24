package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.domain.coupon.response.RegisteredCouponResponse;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CouponService {

    private final Map<Long, Coupon> coupons = new ConcurrentHashMap<>();
    private final Map<Long, Integer> couponDiscounts = new HashMap<>();
    public CouponService() {
        // 쿠폰 ID, 할인 금액
        couponDiscounts.put(100L, 1000);
        couponDiscounts.put(200L, 2000);
    }
    private final Map<Long, Set<Long>> memberCoupons = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> usedCoupons = new ConcurrentHashMap<>();
    private final Map<Long, Integer> couponQuantity = new ConcurrentHashMap<>();

    // synchronized로 동시성 막음 (실제로는 분산락 써야함)
    public synchronized boolean issueCoupon(Long memberId, Long couponId) {
        Coupon coupon = coupons.get(couponId);
        if (coupon == null) return false;
        if (!coupon.isActive()) return false;

        if (couponQuantity.getOrDefault(couponId, 0) <= 0) return false;

        Set<Long> ownedCoupons = memberCoupons.getOrDefault(memberId, new HashSet<>());
        if (ownedCoupons.contains(couponId)) return false; // 중복 발급 불가

        ownedCoupons.add(couponId);
        memberCoupons.put(memberId, ownedCoupons);
        couponQuantity.put(couponId, couponQuantity.get(couponId) - 1);
        return true;
    }

    public List<OwnedCouponResponse> getOwnedCoupons(Long memberId) {
        Set<Long> owned = memberCoupons.getOrDefault(memberId, Collections.emptySet());
        List<OwnedCouponResponse> list = new ArrayList<>();

        for(Long cId : owned) {
            Coupon coupon = coupons.get(cId);
            if(coupon == null) continue;
            boolean expired = LocalDateTime.now().isAfter(coupon.getValidTo());
            list.add(new OwnedCouponResponse(cId, coupon.getName(), expired));
        }

        return list;
    }

    public List<RegisteredCouponResponse> getRegisteredCoupons() {
        List<RegisteredCouponResponse> list = new ArrayList<>();
        for(Coupon c : coupons.values()) {
            boolean active = c.isActive();
            int remain = couponQuantity.getOrDefault(c.getId(), 0);
            list.add(new RegisteredCouponResponse(c.getId(), c.getName(), active, remain));
        }
        return list;
    }

    public boolean useCoupon(Long memberId, Long couponId) {
        Coupon coupon = coupons.get(couponId);
        if (coupon == null || !coupon.isActive()) return false;

        Set<Long> owned = memberCoupons.getOrDefault(memberId, Collections.emptySet());
        if (!owned.contains(couponId)) return false; // 보유하지 않음

        Set<Long> used = usedCoupons.getOrDefault(memberId, new HashSet<>());
        if (used.contains(couponId)) return false; // 이미 사용함

        // 사용 처리
        used.add(couponId);
        usedCoupons.put(memberId, used);
        return true;
    }

    public int applyCoupon(Long memberId, Long couponId) {
        int discount = couponDiscounts.getOrDefault(couponId, 0);
        System.out.println("쿠폰 적용: " + couponId + " -> -" + discount);
        return discount;
    }

    public void rollback(Long memberId, Long couponId) {
        // 실제로는 쿠폰 상태를 '사용 안 함'으로 되돌리는 로직이 들어감
        System.out.println("쿠폰 롤백 처리: memberId = " + memberId + ", couponId = " + couponId);
    }
}
