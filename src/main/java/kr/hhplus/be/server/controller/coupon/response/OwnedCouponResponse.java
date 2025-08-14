package kr.hhplus.be.server.controller.coupon.response;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OwnedCouponResponse {
    private Long couponId;
    private String couponName;
    private boolean isExpired; // 만료 여부

    public static OwnedCouponResponse from(Coupon coupon) {
        boolean expired = LocalDateTime.now().isAfter(coupon.getValidTo());
        return new OwnedCouponResponse(coupon.getId(), coupon.getName(), expired);
    }
}
