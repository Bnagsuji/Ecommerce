package kr.hhplus.be.server.service.coupon;

import kr.hhplus.be.server.controller.coupon.response.IssueCouponResponse;

public interface RedisCouponService {

    IssueCouponResponse issueAsync(Long couponId, Long userId);

    void ensureCouponLoaded(Long couponId);
}
