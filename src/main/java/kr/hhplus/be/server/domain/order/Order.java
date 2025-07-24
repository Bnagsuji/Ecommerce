package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long totalAmount;
    private LocalDateTime orderDate;
    private OrderStatus status;

    @Transient
    private List<OrderItem> orderItems;

    @Builder
    private Order(Long id, Long userId, Long totalAmount, LocalDateTime orderDate, OrderStatus status, List<OrderItem> orderItems) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
        this.orderItems = orderItems;
    }

    public static Order create(OrderRequest orderRequest, long totalOrderAmount, List<OrderItem> newOrderItems) {
        return Order.builder()
                .userId(orderRequest.getUserId())
                .totalAmount(totalOrderAmount)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.COMPLETED)
                .orderItems(newOrderItems)
                .build();
    }

//    // 주문에 아이템 추가 헬퍼 메서드 (양방향 관계 설정 시 유용)
//    public void addOrderItem(OrderItem orderItem) {
//        orderItems.add(orderItem);
//    }
}