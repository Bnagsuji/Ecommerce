package kr.hhplus.be.server.domain.coupon.requeset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueCouponRequest {
    private Long memberId;
    private Long couponId;
}
