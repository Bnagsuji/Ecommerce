package kr.hhplus.be.server.controller.coupon.response;

import kr.hhplus.be.server.domain.coupon.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisteredCouponResponse {
    private Long couponId;
    private String couponName;
    private boolean active;
    private int remainingQuantity;

    public static RegisteredCouponResponse from(Coupon coupon, int remainingQuantity) {
        return new RegisteredCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.isActive(),
                remainingQuantity
        );
    }
}
