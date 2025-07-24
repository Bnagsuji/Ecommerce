package kr.hhplus.be.server.controller.coupon.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisteredCouponResponse {
    private Long couponId;
    private String couponName;
    private boolean active;
    private int remainingQuantity;
}
