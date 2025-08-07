package kr.hhplus.be.server.controller.order.response;

import kr.hhplus.be.server.domain.order.Order;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderResponse(
        Long orderId,
        Long userId,
        Long totalAmount,
        String orderDate,
        String status,
        List<OrderItemResponse> orderedItems
) {

    public record OrderItemResponse(
            Long itemId,
            Long productId,
            Long price,
            int quantity,
            Long itemTotalAmount
    ) {}


    public static OrderResponse of(Order order, List<OrderItemResponse> items) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate().toString())
                .status(order.getStatus().name())
                .orderedItems(items)
                .build();
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getTotalAmount()
                ))
                .toList();

        return of(order, items);
    }
}
