package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long productId;

    private Long price;

    private Integer quantity;

    private Long totalAmount;

    @Column(name = "order_id")
    private Long orderId;

    @Builder
    private OrderItem(Long productId, Long price, Integer quantity) {
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = price * quantity;
    }

//    public static OrderItem of(ProductResponse product, int quantity) {
//        return OrderItem.builder()
//                .productName(product.name())
//                .price(product.price())
//                .quantity(quantity)
//                .build();
//    }

    public static OrderItem of(OrderRequest.OrderItem request, ProductResponse product) {
        if (product.stock() < request.getQuantity()) {
            throw new IllegalArgumentException("재고 부족: " + product.name());
        }

        return OrderItem.builder()
                .productId(product.id())
                .price(product.price())
                .quantity(request.getQuantity())
                .build();
    }



//    public OrderResponse.OrderItemResponse toResponse(Long productId) {
//        return new OrderResponse.OrderItemResponse(
//                productId,
//                productName,
//                price,
//                quantity,
//                totalAmount
//        );
//    }
}
