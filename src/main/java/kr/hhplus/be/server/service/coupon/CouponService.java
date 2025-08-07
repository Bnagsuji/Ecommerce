package kr.hhplus.be.server.service.coupon;

import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;

import java.util.List;

public interface CouponService {

    boolean issueCoupon(Long userId, Long couponId);

    List<OwnedCouponResponse> getOwnedCoupons(Long userId);

    List<RegisteredCouponResponse> getRegisteredCoupons();

    boolean useCoupon(Long userId, Long couponId);

    int applyCoupon(Long userId, Long couponId);

    void rollback(Long userId, Long couponId);
}
