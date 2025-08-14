package kr.hhplus.be.server.service.order;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderHistoryResponse;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;

import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest orderRequest);

    List<ProductResponse> deductProductStock(List<OrderRequest.OrderItem> orderItems);

    Map<Long, ProductResponse> mapProductResponses(List<ProductResponse> responses);

    List<OrderItem> createOrderItems(List<OrderRequest.OrderItem> orderItems, Map<Long, ProductResponse> productMap);

    long calculateTotalAmount(List<OrderItem> orderItems);

    AccountResponse deductBalance(Long userId, long amount);

    Order createOrder(OrderRequest request, long totalAmount, List<OrderItem> orderItems);

    Order saveOrder(Order order);

    void notifyExternalSystem(Order order);

    OrderResponse buildOrderResponse(Order order);

    Order saveOrderToMockStorage(Order order);

}
