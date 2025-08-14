package kr.hhplus.be.server.controller.order.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.hhplus.be.server.domain.order.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
public record OrderResponse(
        Long orderId,
        Long userId,
        Long totalAmount,
        String orderDate,
        String status, // 주문 상태 (예: "COMPLETED", "CANCELLED")
        List<OrderItemResponse> orderedItems // 주문된 각 상품 항목의 상세 목록
) {


    public record OrderItemResponse(
            Long productId,
            String productName,
            Long price,
            int quantity,
            Long itemTotalAmount //총 금액 (가격 * 수량)
    ) {}

    public static OrderResponse of(Order savedOrder, List<OrderItemResponse> orderItemResponses) {
        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .orderDate(savedOrder.getOrderDate().toString())
                .status(savedOrder.getStatus().name())
                .orderedItems(orderItemResponses)
                .build();
    }
}