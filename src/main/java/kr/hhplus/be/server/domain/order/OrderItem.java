package kr.hhplus.be.server.domain.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private Long price;
    private int quantity;
    private Long totalAmount;

    @Builder
    private OrderItem(Long id, String productName, Long price, int quantity) {
        this.id = id;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        calculateItemTotal();
    }

    public static OrderItem of(ProductResponse updatedProductResponse, int requestedQuantity) {
        return OrderItem.builder()
                .id(updatedProductResponse.id())
                .productName(updatedProductResponse.name())
                .price(updatedProductResponse.price())
                .quantity(requestedQuantity)
                .build();
    }

    private void calculateItemTotal() {
        if (this.price != null) {
            this.totalAmount = this.price * this.quantity;
        } else {
            this.totalAmount = 0L;
        }
    }

    public OrderResponse.OrderItemResponse toResponse() {
        return new OrderResponse.OrderItemResponse(id, productName, price, quantity, totalAmount);
    }
}
