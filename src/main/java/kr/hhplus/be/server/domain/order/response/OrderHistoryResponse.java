package kr.hhplus.be.server.domain.order.response;

public record OrderHistoryResponse(
        Long id,          // 상품 ID
        String name,      // 상품명 (필요하면)
        int quantity,            // 주문 수량
        String orderedDate        // 주문일시 (String 또는 LocalDateTime)
) {}