package kr.hhplus.be.server.integration.product;

import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProductConcurrencyTest {

    //레디스 분산락 동시성 테스트 완료

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    private Long productId;

    @BeforeEach
    void setup() {
        // 상품 생성 및 재고 5개 설정

       Product product = Product.builder()
                .name("동시성테스트 상품")
                .stock(5)
                .build();
        productId = productRepository.save(product).getId();
        System.out.println("동시성테스트 상품"+productId+" :  "+product.getStock());

    }

    @Test
    void 재고수만큼만_동시주문_성공하고_나머지는_실패해야함() throws InterruptedException {
        // given
        int initialStock = 5;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ProductStockRequest req = new ProductStockRequest(productId, 1);
                    try {
                        productService.useProduct(List.of(req));
                        results.add(true);
                    } catch (Exception e) {
                        results.add(false);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long successCount = results.stream().filter(Boolean::booleanValue).count();

        Product updatedProduct = productRepository.findById(productId).orElseThrow();

        assertThat(successCount).isEqualTo(initialStock);
        assertThat(updatedProduct.getStock()).isEqualTo(0);
    }


    @Test
    void 재고보다_많이_요청하면_예외발생해야함() {
        // given
        Product product = Product.builder()
                .name("재고 부족 테스트")
                .stock(3)
                .build();
        Long id = productRepository.save(product).getId();

        ProductStockRequest request = new ProductStockRequest(id, 4); // 재고보다 많은 요청

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
                    productService.useProduct(List.of(request));
                }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고 부족");
    }

    @Test
    void 재고소진후_더이상_감소되지않아야함() throws InterruptedException {
        // given
        Product product = Product.builder()
                .name("재고 소진 테스트")
                .stock(3)
                .build();
        Long id = productRepository.save(product).getId();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    try {
                        productService.useProduct(List.of(new ProductStockRequest(id, 1)));
                        results.add(true);
                    } catch (Exception e) {
                        results.add(false);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long success = results.stream().filter(Boolean::booleanValue).count();
        Product updated = productRepository.findById(id).orElseThrow();

        assertThat(success).isEqualTo(3);  // 최대 재고만큼만 성공
        assertThat(updated.getStock()).isEqualTo(0);
    }

    @Test
    void 여러상품_동시차감_정상작동해야함() throws InterruptedException {
        Product p1 = productRepository.save(Product.builder().name("P1").stock(3).build());
        Product p2 = productRepository.save(Product.builder().name("P2").stock(3).build());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    productService.useProduct(List.of(
                            new ProductStockRequest(p1.getId(), 1),
                            new ProductStockRequest(p2.getId(), 1)
                    ));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Product updatedP1 = productRepository.findById(p1.getId()).orElseThrow();
        Product updatedP2 = productRepository.findById(p2.getId()).orElseThrow();

        assertThat(updatedP1.getStock()).isEqualTo(0);
        assertThat(updatedP2.getStock()).isEqualTo(0);
    }




}
