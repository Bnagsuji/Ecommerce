package kr.hhplus.be.server.product;


import kr.hhplus.be.server.controller.product.ProductController;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.service.product.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private ProductServiceImpl mockProductServiceImpl;

    @Test
    void 상품_상세조회_성공_테스트() throws Exception {
        // given
        Long productId = 1L;
        ProductResponse mockResponse = new ProductResponse(
                productId,
                "Test Product",
                5000,
                100,
                LocalDateTime.now()
        );

        given(mockProductServiceImpl.getProductById(productId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/product/{id}", productId))
                .andDo(print())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(5000L))
                .andExpect(jsonPath("$.stock").value(100));
    }

    @Test
    void 판매량_상위_상품조회_성공() throws Exception {
        // given
        List<ProductResponse> mockList = List.of(
                new ProductResponse(1L, "상품A", 1000, 50, LocalDateTime.now()),
                new ProductResponse(2L, "상품B", 2000, 30, LocalDateTime.now())
        );

        given(mockProductServiceImpl.getTopSellingProducts()).willReturn(mockList);

        // when & then
        mockMvc.perform(get("/api/product/top-selling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("상품A"))
                .andExpect(jsonPath("$[1].name").value("상품B"));
    }
}
