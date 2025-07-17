package kr.hhplus.be.server.domain.product;

import kr.hhplus.be.server.domain.order.request.OrderRequest;
import kr.hhplus.be.server.domain.order.response.OrderHistoryResponse;
import kr.hhplus.be.server.domain.product.response.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

//목서비스
@Service
public class MockProductService {

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

    /* 상품 상세 조회 */
    public ProductResponse getProductById(Long id) {
        for (ProductResponse product : mockProducts) {
            if (product.id().equals(id)) {
                return product;
            }
        }
        throw new IllegalArgumentException("해당 상품이 없습니다. id=" + id);
    }


    public List<ProductResponse> getTopSellingProducts() {
        // 1. 상품별 판매 수량 집계
        Map<Long, Integer> salesMap = new HashMap<>();
        for (OrderHistoryResponse history : mockOrders) {
            Long productId = history.id();
            int quantity = history.quantity();
            salesMap.put(productId, salesMap.getOrDefault(productId, 0) + quantity);
        }

        // 2. 판매량 기준으로 정렬
        List<Long> topProductIds = salesMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 3. 정렬된 ID로 상품 조회
        List<ProductResponse> result = new ArrayList<>();
        for (Long productId : topProductIds) {
            for (ProductResponse product : mockProducts) {
                if (product.id().equals(productId)) {
                    result.add(product);
                    break;
                }
            }
        }

        return result;
    }

    // 재고 차감 메서드
    public void decreaseStock(Long productId, int quantity) {
        for (int i = 0; i < mockProducts.size(); i++) {
            ProductResponse p = mockProducts.get(i);
            if (p.id().equals(productId)) {
                if (p.stock() < quantity) {
                    throw new IllegalStateException("재고 부족: 현재 " + p.stock() + "개");
                }
                // 새 객체로 대체 (record는 불변이니까)
                ProductResponse updated = new ProductResponse(
                        p.id(),
                        p.name(),
                        p.price(),
                        p.stock() - quantity,
                        p.regDate()
                );
                mockProducts.set(i, updated);
                return;
            }
        }
        throw new NoSuchElementException("상품을 찾을 수 없습니다.");
    }

    public void rollback(List<OrderRequest.OrderItem> items) {
        for (OrderRequest.OrderItem item : items) {
            // 롤백 시 재고 다시 증가시킴 (단순 처리)
            increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    public void increaseStock(Long productId, int quantity) {
        // 실제로는 DB에서 재고 증가
        System.out.println("재고 롤백: productId = " + productId + ", 수량 = " + quantity);
    }

}
