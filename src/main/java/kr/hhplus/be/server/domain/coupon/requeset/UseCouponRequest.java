package kr.hhplus.be.server.domain.coupon.requeset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UseCouponRequest {
    private Long memberId;
    private Long couponId;
}