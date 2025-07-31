package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderDate;

    // 단방향
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(Long userId, List<OrderItem> orderItems, OrderStatus status) {
        this.userId = userId;
        this.status = status;
        this.orderDate = LocalDateTime.now();
        this.totalAmount = calculateTotalAmount(orderItems);
    }

    public static Order create(OrderRequest req, List<OrderItem> orderItems) {
        return Order.builder()
                .userId(req.getUserId())
                .status(OrderStatus.COMPLETED)
                .orderItems(orderItems)
                .build();
    }


    public static long calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .mapToLong(OrderItem::getTotalAmount)
                .sum();
    }
}
