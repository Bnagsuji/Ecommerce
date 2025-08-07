package kr.hhplus.be.server.integration.coupon;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.service.coupon.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponConcurrencyTest {

    @Autowired
    private CouponJpaRepository couponRepository;

    @Autowired
    private UserCouponJpaRepository userCouponRepository;

    @Autowired
    private CouponService couponService;

    private Long couponId;

    @BeforeEach
    @Transactional
    void setUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();

        Coupon coupon = Coupon.builder()
                .name("TEST")
                .validFrom(LocalDateTime.now().minusMinutes(1))
                .validTo(LocalDateTime.now().plusMinutes(10))
                .quantity(100)
                .discountAmount(1000)
                .build();
        couponRepository.save(coupon);
        this.couponId = coupon.getId();
    }

    @Test
    @DisplayName("동시에 100명이 쿠폰을 발급받는 경우 - 발급 수량이 정확히 100이어야 한다")
    void testIssueCouponConcurrency() throws InterruptedException {
        int threadCount = 100;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Long userId = (long) i;
            service.execute(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        service.shutdown();

        List<UserCoupon> issued = userCouponRepository.findAll();
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();

        assertThat(issued.size()).isEqualTo(100);
        assertThat(coupon.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("동일한 유저가 동시에 여러번 요청했을 때 중복 발급되지 않아야 한다")
    void testDuplicateIssueBySameUser() throws InterruptedException {
        int threadCount = 10;
        Long userId = 1L;

        ExecutorService service = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            service.execute(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        service.shutdown();

        List<UserCoupon> userCoupons = userCouponRepository.findAllByUserId(userId);
        assertThat(userCoupons).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 모두 소진된 후 추가 요청은 실패해야 한다")
    void testExceedingCouponRequests() throws InterruptedException {
        // 수량 100개이므로 110명이 요청했을 때 10명은 실패해야 함
        int threadCount = 110;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Long userId = (long) i;
            service.execute(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        service.shutdown();

        List<UserCoupon> issued = userCouponRepository.findAll();
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();

        assertThat(issued.size()).isEqualTo(100);
        assertThat(coupon.getQuantity()).isEqualTo(0);
    }
}
















