package kr.hhplus.be.server.integration.product;

import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.service.product.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductTopSellingCacheTest {

    @Autowired
    private ProductServiceImpl productService;

    @SpyBean
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate  redisTemplate;

    private static final String CACHE_KEY = "topSelling::d5:L3";

    @Autowired
    RedisConnectionFactory cf;

    @BeforeEach
    void flush() {
        cf.getConnection().serverCommands().flushDb();
    }
    @Test
    void getTopSellingProducts_첫호출은_DB조회하고_두번째호출은_캐시조회() {
        // given
        Product dummy = Product.builder().
                name("상품")
                .stock(10)
                .price(1000L)
                .build();

        Mockito.when(productRepository.findTopSellingList(5, 3))
                .thenReturn(List.of(dummy));

        // when - 첫 호출 (캐시 미스 → DB 조회)
        List<ProductResponse> firstCall = productService.getTopSellingProducts();

        // then
        assertThat(firstCall).hasSize(1);
        assertThat(redisTemplate.hasKey(CACHE_KEY)).isTrue();
        verify(productRepository, times(1)).findTopSellingList(5, 3);

        // when - 두 번째 호출 (캐시 히트 → DB 미조회)
        List<ProductResponse> secondCall = productService.getTopSellingProducts();

        // then
        assertThat(secondCall).hasSize(1);
        verify(productRepository, times(1)).findTopSellingList(5, 3); // 여전히 1번만 호출됨
    }

    @Test
    void 캐시된데이터는_TTL만료후_다시_DB조회된다() throws InterruptedException {
        // given
        Product dummy = Product.builder().
                name("상품")
                .stock(10)
                .price(1000L)
                .build();
        Mockito.when(productRepository.findTopSellingList(5, 3))
                .thenReturn(List.of(dummy));

        // when - 첫 호출 (캐시 저장)
        productService.getTopSellingProducts();
        verify(productRepository, times(1)).findTopSellingList(5, 3);

        // TTL(30초) 테스트 위해 31초 대기 (설정된 TTL보다 길게)
        Thread.sleep(31_000);

        // then - 다시 호출 시 DB 조회 발생
        productService.getTopSellingProducts();
        verify(productRepository, times(2)).findTopSellingList(5, 3);
    }
}
