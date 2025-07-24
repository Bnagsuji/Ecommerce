package kr.hhplus.be.server.domain.order.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private Long memberId;
    private List<OrderItem> items;
    private Long couponId;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem {
        private Long productId;
        private int quantity;
    }
}