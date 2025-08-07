package kr.hhplus.be.server.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

/*    @Test
    void isActive_현재_유효기간_내_이면_true() {
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = new Coupon(
                "테스트쿠폰",
                now.minusDays(1),
                now.plusDays(1)
        );

        assertThat(coupon.isActive()).isTrue();
    }

    @Test
    void isActive_유효기간_시작_전이면_false() {
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = new Coupon(
                "테스트쿠폰",
                now.plusDays(1),
                now.plusDays(2)
        );

        assertThat(coupon.isActive()).isFalse();
    }

    @Test
    void isActive_유효기간_종료_후면_false() {
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = new Coupon(
                "테스트쿠폰",
                now.minusDays(3),
                now.minusDays(1)
        );

        assertThat(coupon.isActive()).isFalse();
    }

    @Test
    void isActive_경계값_테스트() {
        LocalDateTime now = LocalDateTime.now();

        Coupon couponStart = new Coupon(
                "시작경계쿠폰",
                now,
                now.plusDays(1)
        );
        Coupon couponEnd = new Coupon(
                "종료경계쿠폰",
                now.minusDays(1),
                now
        );

        // isAfter, isBefore 이므로 시작 시각 == now 면 false
        assertThat(couponStart.isActive()).isFalse();
        assertThat(couponEnd.isActive()).isFalse();
    }*/
}
