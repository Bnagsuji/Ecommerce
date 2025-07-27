package kr.hhplus.be.server.order;


import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.OrderItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    void calculateItemTotal_정상_계산_테스트() {
        // price, quantity 넣고 totalAmount 계산 검증
        OrderItem item = OrderItem.builder()
                .price(3000L)
                .quantity(3)
                .build();

        assertThat(item.getTotalAmount()).isEqualTo(3000L * 3);
    }

    @Test
    void calculateItemTotal_null가격_0으로_처리_테스트() {
        OrderItem item = OrderItem.builder()
                .price(null)
                .quantity(5)
                .build();

        assertThat(item.getTotalAmount()).isEqualTo(0L);
    }

    @Test
    void of_메서드_테스트() {
        ProductResponse product = new ProductResponse(1L, "테스트상품", 2000L, 10, null);
        int quantity = 4;

        OrderItem item = OrderItem.of(product, quantity);

        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("테스트상품");
        assertThat(item.getPrice()).isEqualTo(2000L);
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getTotalAmount()).isEqualTo(2000L * 4);
    }

    @Test
    void toResponse_변환_테스트() {
        OrderItem item = OrderItem.builder()
                .id(5L)
                .productName("변환상품")
                .price(4000L)
                .quantity(2)
                .build();

        OrderResponse.OrderItemResponse response = item.toResponse();

        assertThat(response.productId()).isEqualTo(5L);
        assertThat(response.productName()).isEqualTo("변환상품");
        assertThat(response.price()).isEqualTo(4000L);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.itemTotalAmount()).isEqualTo(4000L * 2);
    }
}

