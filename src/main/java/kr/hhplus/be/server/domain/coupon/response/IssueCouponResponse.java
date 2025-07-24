package kr.hhplus.be.server.domain.coupon.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssueCouponResponse {
    private boolean success;
    private String message;
}