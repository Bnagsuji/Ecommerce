package kr.hhplus.be.server.coupon;

import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.service.coupon.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {
    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl();

        couponService.getCouponMap().put(999L, new Coupon(999L, "테스트쿠폰", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
        couponService.getCouponQuantities().put(999L, 1);
    }

    @Test
    void 쿠폰_정상_발급() {
        Long userId = 1L;
        Long couponId = 100L;

        boolean result = couponService.issueCoupon(userId, couponId);

        assertTrue(result);
    }

    @Test
    void 중복_쿠폰_발급_차단() {
        Long userId = 1L;
        Long couponId = 100L;

        couponService.issueCoupon(userId, couponId); // 첫 발급
        boolean result = couponService.issueCoupon(userId, couponId); // 중복 발급

        assertFalse(result);
    }

    @Test
    void 수량_초과_발급_불가() {
        Long couponId = 100L; // 수량 3개 설정됨

        // 3명에게 발급
        assertTrue(couponService.issueCoupon(1L, couponId));
        assertTrue(couponService.issueCoupon(2L, couponId));
        assertTrue(couponService.issueCoupon(3L, couponId));

        // 4번째는 실패해야 함
        assertFalse(couponService.issueCoupon(4L, couponId));
    }

    @Test
    void 비활성_쿠폰_발급_불가() {
        // 유효기간 지난 쿠폰 임의로 추가
        Long expiredCouponId = 999L;
        couponService.getCouponMap().put(expiredCouponId, new Coupon(
                expiredCouponId, "만료쿠폰", LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1))
        );
        couponService.getCouponQuantities().put(expiredCouponId, 5);

        boolean result = couponService.issueCoupon(1L, expiredCouponId);

        assertFalse(result);
    }

    @Test
    void 소유한_쿠폰_조회() {
        Long userId = 1L;
        Long couponId = 100L;
        couponService.issueCoupon(userId, couponId);

        List<OwnedCouponResponse> result = couponService.getOwnedCoupons(userId);

        assertEquals(1, result.size());
        assertEquals(couponId, result.get(0).getCouponId());
    }

    @Test
    void 등록된_쿠폰_조회() {
        List<RegisteredCouponResponse> result = couponService.getRegisteredCoupons();

        assertEquals(3, result.size());
    }

    @Test
    void 쿠폰_정상_사용_테스트() {
        Long userId = 1L;
        Long couponId = 100L;
        couponService.issueCoupon(userId, couponId);

        boolean result = couponService.useCoupon(userId, couponId);

        assertTrue(result);
    }

    @Test
    void 쿠폰_중복_사용_불가_테스트() {
        Long userId = 1L;
        Long couponId = 100L;
        couponService.issueCoupon(userId, couponId);
        couponService.useCoupon(userId, couponId);

        boolean result = couponService.useCoupon(userId, couponId);

        assertFalse(result);
    }

    @Test
    void 할인금액_정상_반환_테스트() {
        int discount = couponService.applyCoupon(1L, 100L);
        assertEquals(1000, discount);
    }

    @Test
    void 존재하지_않는_쿠폰_할인금액_0_테스트() {
        int discount = couponService.applyCoupon(1L, 999L);
        assertEquals(0, discount);
    }

    @Test
    void 쿠폰_사용_롤백_테스트() {
        Long userId = 1L;
        Long couponId = 100L;

        couponService.issueCoupon(userId, couponId);
        couponService.useCoupon(userId, couponId);

        couponService.rollback(userId, couponId);

        // 롤백 후 다시 사용 가능해야 함
        boolean result = couponService.useCoupon(userId, couponId);
        assertTrue(result);
    }
}
