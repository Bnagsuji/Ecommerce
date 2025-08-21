package kr.hhplus.be.server.service.ranking;

import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.OrderItem;

import java.util.List;

public interface ProductRankingService {
    void incrOrder(List<OrderItem> items);

    List<ProductResponse> getTopForRecentDaysPipelined(int days, int limit);
}
