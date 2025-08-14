package kr.hhplus.be.server.integration.order;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.usecase.order.PlaceOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderRedisConcurrencyTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    Long userId1 = 1001L;
    Long userId2 = 1002L;
    Long productId = 3000L;
    Long couponId = null;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        orderRepository.deleteAll();
        productJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
        couponRepository.deleteAll();

        Product product = productJpaRepository.save(Product.builder()
                .name("Test 상품")
                .price(1000L)
                .stock(1)
                .regDate(LocalDateTime.now())
                .build());
        productId = product.getId();

        accountJpaRepository.save(Account.create(userId1, 10000L));
        accountJpaRepository.save(Account.create(userId2, 10000L));

        Coupon coupon = couponRepository.save(Coupon.builder()
                .name("1000원 할인 쿠폰")
                .quantity(10)
                .discountAmount(1000)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .build());

        couponId = coupon.getId();

        // 쿠폰 사전 발급 처리
        userCouponRepository.save(new UserCoupon(userId1, coupon));
        userCouponRepository.save(new UserCoupon(userId2, coupon));
    }

    @Test
    void 중복요청_같은유저_중복방지되어야함() throws InterruptedException {
        Long userId = userId1;
        int threadCount = 5;

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    placeOrderUseCase.placeOrder(new OrderRequest(
                            userId,
                            null,
                            List.of(new OrderRequest.OrderItem(productId, 1))
                    ));
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<Order> all = orderRepository.findAll();
        assertThat(all).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void 여러명_동시_쿠폰사용_모두성공_단_재고제한() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Long> userIds = List.of(userId1, userId2);

        for (Long userId : userIds) {
            executor.submit(() -> {
                try {
                    placeOrderUseCase.placeOrder(new OrderRequest(
                            userId,
                            couponId,
                            List.of(new OrderRequest.OrderItem(productId, 1))
                    ));
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<Order> all = orderRepository.findAll();
        assertThat(all.size()).isLessThanOrEqualTo(1);
    }
}
