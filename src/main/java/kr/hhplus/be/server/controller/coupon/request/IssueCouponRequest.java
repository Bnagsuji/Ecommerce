package kr.hhplus.be.server.controller.coupon.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueCouponRequest {
    private Long userId;
    private Long couponId;
}
