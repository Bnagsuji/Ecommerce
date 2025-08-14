package kr.hhplus.be.server.service.product;

import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.controller.order.response.OrderHistoryResponse;
import kr.hhplus.be.server.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


//목서비스
@Service
@RequiredArgsConstructor
public class MockProductService {

    private final ProductRepository productRepository;

    //상품 리스트
    private final List<ProductResponse> mockProducts = List.of(
            new ProductResponse(1L, "맥북 M3", 2000000, 5, "2025-07-01 10:00:00"),
            new ProductResponse(2L, "갤럭시 Z 플립", 1300000, 3, "2025-07-10 15:30:00"),
            new ProductResponse(3L, "아이패드 프로", 0, 0, "2025-07-15 08:00:00")
    );

    //주문 히스토리
    private final List<OrderHistoryResponse> mockOrders = List.of(
            new OrderHistoryResponse(1L, "맥북 M3", 1, "2025-07-15 10:00:00"),
            new OrderHistoryResponse(1L, "맥북 M3", 2, "2025-07-16 12:00:00"),
            new OrderHistoryResponse(1L, "맥북 M3", 3, "2025-07-16 09:00:00"),
            new OrderHistoryResponse(3L, "아이패드 프로", 1, "2025-07-17 08:30:00"),
            new OrderHistoryResponse(3L, "아이패드 프로", 1, "2025-07-14 08:00:00")
    );

    //상위 판매 상품 리스트 
    private final List<ProductResponse> top3selling = List.of(
            new ProductResponse(2L, "갤럭시 Z 플립", 2000000, 5, "2025-07-01 10:00:00"),
            new ProductResponse(1L, "맥북 M3", 1300000, 3, "2025-07-10 15:30:00"),
            new ProductResponse(3L, "아이패드 프로", 0, 0, "2025-07-15 08:00:00")
    );



    /* 상품 상세 조회 */
    public ProductResponse getProductById(Long id) {
        ProductResponse mock = new ProductResponse(
                1L,
                "맥북 M3",
                2000000,
                5,
                "2025-07-01 10:00:00"
        );

        if(id.equals(mock.id())) {
            return mock;
        }

        throw new IllegalArgumentException("해당 상품이 존재하지 않습니다.");
    }


    /* 상위 판매 상품 리스트 조회 */
    public List<ProductResponse> getTopSellingProducts() {
        return top3selling;
    }

}
