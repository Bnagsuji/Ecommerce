package kr.hhplus.be.server.domain.coupon.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnedCouponResponse {
    private Long couponId;
    private String couponName;
    private boolean isExpired; // 만료 여부
}
