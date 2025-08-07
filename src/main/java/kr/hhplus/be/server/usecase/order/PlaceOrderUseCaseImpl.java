package kr.hhplus.be.server.usecase.order;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final ProductService productService;
    private final AccountService accountService;
    private final OrderRepository orderRepository;

    @Override
    public OrderResponse placeOrder(OrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }
        List<ProductResponse> productResponses = handleStock(req.getItems());
        Map<Long, ProductResponse> productMap = ProductResponse.toMapById(productResponses);
        List<OrderItem> orderItems = createOrderItems(req, productMap);
        long totalAmount = Order.calculateTotalAmount(orderItems);
        handlePayment(req.getUserId(), totalAmount);
        Order order = Order.create(req, orderItems);
        Order savedOrder = orderRepository.save(order);
        System.out.println("Saved orderItems size after save: " + savedOrder.getOrderItems().size());
        return OrderResponse.from(savedOrder);
    }

    private List<ProductResponse> handleStock(List<OrderRequest.OrderItem> items) {
        List<ProductStockRequest> requests = items.stream()
                .map(OrderRequest.OrderItem::toStockRequest)
                .toList();

        return productService.useProduct(requests);
    }

    private List<OrderItem> createOrderItems(OrderRequest req, Map<Long, ProductResponse> productMap) {
        return req.getItems().stream()
                .map(item -> {
                    ProductResponse product = productMap.get(item.getProductId());
                    if (product == null) {
                        throw new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + item.getProductId());
                    }
                    if (product.stock() < item.getQuantity()) {
                        throw new IllegalArgumentException("재고 부족: 상품 ID " + item.getProductId());
                    }
                    return OrderItem.of(item, product);
                })
                .toList();
    }

    private void handlePayment(Long userId, long totalAmount) {
        AccountResponse response = accountService.useBalance(userId, totalAmount);
        if (response.amount() < 100L) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
    }

}
