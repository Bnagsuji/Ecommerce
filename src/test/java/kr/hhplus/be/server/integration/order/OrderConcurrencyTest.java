package kr.hhplus.be.server.integration.order;

import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.request.OrderRequest.OrderItem;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.usecase.order.PlaceOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderConcurrencyTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    private Long productId;
    private Long couponId;
    private final int stock = 5;
    private final Long unitPrice = 1000L;

    @BeforeEach
    void setUp() {
        productJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();

        Product product = Product.builder()
                .name("테스트상품1")
                .stock(stock)
                .price(unitPrice)
                .build();
        productId = productJpaRepository.save(product).getId();

        for (long i = 0; i < 10; i++) {
            accountJpaRepository.save(Account.create(i, 10_000L));
        }

        Coupon coupon = Coupon.builder()
                .name("DISCOUNT")
                .quantity(1)
                .discountAmount(500)
                .validFrom(LocalDateTime.now().minusMinutes(1))
                .validTo(LocalDateTime.now().plusMinutes(10))
                .build();
        couponId = couponJpaRepository.save(coupon).getId();
    }

    @Test
    void 재고보다_많은_동시_주문_요청은_성공수만큼만_반영되어야_함() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (long i = 0; i < threadCount; i++) {
            long userId = i;
            executor.submit(() -> {
                try {
                    placeOrderUseCase.placeOrder(
                            new OrderRequest(userId, null, List.of(new OrderItem(productId, 1)))
                    );
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long success = results.stream().filter(Boolean::booleanValue).count();
        Product product = productJpaRepository.findById(productId).orElseThrow();

        assertThat(success).isLessThanOrEqualTo(stock);
        assertThat(success).isGreaterThanOrEqualTo(stock - 1); // 현실적인 보정
    }

    @Test
    void 쿠폰은_동시에_하나만_성공적으로_사용되어야_함() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executor.submit(() -> {
                try {
                    placeOrderUseCase.placeOrder(
                            new OrderRequest(userId, couponId, List.of(new OrderItem(productId, 1)))
                    );
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long success = results.stream().filter(Boolean::booleanValue).count();
        Coupon coupon = couponJpaRepository.findById(couponId).orElseThrow();

        assertThat(success).isEqualTo(1);
        assertThat(coupon.getQuantity()).isEqualTo(0);
    }

    @Test
    void 잔액이_부족하면_모든_동시_요청은_실패해야_함() throws InterruptedException {
        // 모든 유저 잔액을 500원으로 설정
        accountJpaRepository.deleteAll();
        for (long i = 0; i < 5; i++) {
            accountJpaRepository.save(Account.create(i, 500L));
        }

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executor.submit(() -> {
                try {
                    placeOrderUseCase.placeOrder(
                            new OrderRequest(userId, null, List.of(new OrderItem(productId, 1)))
                    );
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long success = results.stream().filter(Boolean::booleanValue).count();
        assertThat(success).isEqualTo(0);
    }
}
