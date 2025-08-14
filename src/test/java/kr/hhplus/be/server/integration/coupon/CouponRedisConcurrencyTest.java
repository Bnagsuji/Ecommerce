package kr.hhplus.be.server.integration.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.service.coupon.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponRedisConcurrencyTest {

    @Autowired
    CouponService couponService;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    UserCouponJpaRepository userCouponJpaRepository;

    private Long couponId;

    @BeforeEach
    void setUp() {
        userCouponJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();

        Coupon coupon = Coupon.create(
                "테스트쿠폰",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                1,
                1000
        );
        couponId = couponJpaRepository.save(coupon).getId();
    }

    @Test
    void 동시에_여러명_발급요청시_정확히_1명만_성공해야함() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Long[] userIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            userIds[i] = 1000L + i;
        }

        for (Long userId : userIds) {
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long successCount = userCouponJpaRepository.findAll().stream()
                .filter(uc -> uc.getCoupon().getId().equals(couponId))
                .count();

        Coupon coupon = couponJpaRepository.findById(couponId).orElseThrow();

        assertThat(successCount).isEqualTo(1);      // 딱 1명만 발급
        assertThat(coupon.getQuantity()).isEqualTo(0); // 수량은 0
    }

    @Test
    void 동일유저_중복발급요청_1번만_성공해야함() throws InterruptedException {
        // given
        Coupon coupon = Coupon.create(
                "중복방지쿠폰",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                10,
                1000
        );
        Long couponId = couponJpaRepository.save(coupon).getId();

        Long userId = 9999L;

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long count = userCouponJpaRepository.findAll().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getCoupon().getId().equals(couponId))
                .count();

        assertThat(count).isEqualTo(1); // 1번만 성공
    }


}
