package kr.hhplus.be.server.controller.coupon.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UseCouponRequest {
    private Long userId;
    private Long couponId;
}