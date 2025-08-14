package kr.hhplus.be.server.controller.order.request;

import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
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
        private Integer quantity;



        public ProductStockRequest toStockRequest() {
            return new ProductStockRequest(productId, quantity);
        }
    }
}
