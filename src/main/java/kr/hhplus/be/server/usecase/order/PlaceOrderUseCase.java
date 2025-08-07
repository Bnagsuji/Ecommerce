package kr.hhplus.be.server.usecase.order;

import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;

public interface PlaceOrderUseCase {
    OrderResponse placeOrder(OrderRequest req);
}
