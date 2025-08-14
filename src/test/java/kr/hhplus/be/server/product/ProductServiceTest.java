package kr.hhplus.be.server.product;


import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.service.product.impl.ProductServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    ProductServiceImpl mockProductServiceImpl;

    @Mock
    ProductJpaRepository productRepository;

/*
    @Test
    void 상품_상세_조회_성공_테스트() {
        // given - 고정된 시간으로 테스트 객체 만들기
        LocalDateTime fixedTime = LocalDateTime.now();

        ProductResponse origin = new ProductResponse(
                1L,
                "맥북 M3",
                2000000,
                5,
                fixedTime
        );

        // when
        ProductResponse res = mockProductServiceImpl.getProductById(1L);

        // then
        Assertions.assertEquals(origin.id(), res.id());
        Assertions.assertEquals(origin.name(), res.name());
        Assertions.assertEquals(origin.price(), res.price());
        Assertions.assertEquals(origin.stock(), res.stock());
        // regDate는 시간이라 제외

    }


    @Test
    void 상품_상세_조회_시_없음_테스트() {
        //given
        ProductResponse origin = new ProductResponse(
                2L,
                "맥북 M3",
                2000000,
                5,
                LocalDateTime.now()
        );

        //when
        Exception ex =
                assertThrows(IllegalArgumentException.class,()-> mockProductServiceImpl.getProductById(origin.id()));

        //then
        assertEquals("해당 상품이 존재하지 않습니다.",ex.getMessage());
    }

    @Test
    void 상위_판매_상품_리스트_조회_성공_테스트() {
        // given
        List<ProductResponse> origin = List.of(
                new ProductResponse(2L, "갤럭시 Z 플립", 2000000, 5, LocalDateTime.now()),
                new ProductResponse(1L, "맥북 M3", 1300000, 3, LocalDateTime.now()),
                new ProductResponse(3L, "아이패드 프로", 0, 0, LocalDateTime.now())
        );

        // when
        List<ProductResponse> res = mockProductServiceImpl.getTopSellingProducts();

        // then
        assertThat(res)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("regDate")
                .isEqualTo(origin);
    }
*/


}
