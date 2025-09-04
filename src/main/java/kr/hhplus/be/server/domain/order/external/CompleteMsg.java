package kr.hhplus.be.server.domain.order.external;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CompleteMsg (
     Long orderId,
     Long userId,
     List<Item> items,
     LocalDateTime createDate
)

{

    @Getter
    public static class Item {
        private final Long productId;
        private final long quantity;
        public Item(Long productId, long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }

}
