package kr.hhplus.be.server.controller.order.request;

import jakarta.validation.Valid;
import kr.hhplus.be.server.controller.product.request.ProductRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private Long userId;
    private Long couponId;
    private List<OrderItem> items;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem {
        private Long productId;
        private int quantity;

        public ProductRequest toProductRequest() {
            return new ProductRequest(productId, quantity);
        }
    }
}