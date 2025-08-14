package kr.hhplus.be.server.service.order.impl;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderHistoryResponse;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.coupon.CouponService;
import kr.hhplus.be.server.service.external.ExternalPlatformService;
import kr.hhplus.be.server.service.order.OrderService;
import kr.hhplus.be.server.service.product.ProductService;
import kr.hhplus.be.server.service.product.impl.ProductServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final AccountService accountService;
    private final ProductService productService;
    private final CouponService couponService;
    private final ExternalPlatformService externalPlatformService;

    private final Map<Long, Order> mockOrderStorage = new HashMap<>();

    private final List<OrderHistoryResponse> orderHistory = new ArrayList<>();


    //주문 처리
    @Transactional
    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("상품이 존재하지 않습니다.");
        }
        List<ProductResponse> productResponses = deductProductStock(orderRequest.getItems());
        Map<Long, ProductResponse> productMap = mapProductResponses(productResponses);
        List<OrderItem> orderItems = createOrderItems(orderRequest.getItems(), productMap);
        long totalAmount = calculateTotalAmount(orderItems);

        AccountResponse account = deductBalance(orderRequest.getUserId(), totalAmount);

        Order order = createOrder(orderRequest, totalAmount, orderItems);
        Order savedOrder = saveOrder(order);
        notifyExternalSystem(savedOrder);

        return buildOrderResponse(savedOrder);
    }


    // 1. 재고 차감
    @Override
    public List<ProductResponse> deductProductStock(List<OrderRequest.OrderItem> orderItems) {
        List<ProductRequest> requests = orderItems.stream()
                .map(OrderRequest.OrderItem::toProductRequest)
                .toList();
        return productService.useProduct(requests);
    }

    // 2. 상품 응답값 Map에 넣기
    @Override
    public Map<Long, ProductResponse> mapProductResponses(List<ProductResponse> responses) {
        return responses.stream().collect(Collectors.toMap(ProductResponse::id, r -> r));
    }


    // 3. OrderItem 생성
    @Override
    public List<OrderItem> createOrderItems(List<OrderRequest.OrderItem> orderItems, Map<Long, ProductResponse> productMap) {
        return orderItems.stream()
                .map(item -> {
                    ProductResponse product = productMap.get(item.getProductId());
                    if (product.price() <= 0) {
                        throw new IllegalArgumentException("상품 금액이 0 이하입니다.");
                    }
                    return OrderItem.of(product, item.getQuantity());
                }).toList();
    }

    // 4. 총액 계산
    @Override
    public long calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream().mapToLong(OrderItem::getTotalAmount).sum();
    }

    // 6. 잔액 차감
    @Override
    public AccountResponse deductBalance(Long userId, long amount) {
        return accountService.useBalance(userId, amount);
    }


    // 7. 주문 생성
    @Override
    public Order createOrder(OrderRequest request, long totalAmount, List<OrderItem> orderItems) {
        return Order.create(request, totalAmount, orderItems);
    }


    // 8. 저장
    @Override
    public Order saveOrder(Order order) {
        return saveOrderToMockStorage(order); //임시
    }


    // 9. 외부 전송
    @Override
    public void notifyExternalSystem(Order order) {
        externalPlatformService.sendOrderCompletionData(order);
    }

    // 10. 응답 생성
    @Override
    public OrderResponse buildOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItem::toResponse)
                .toList();

        return OrderResponse.of(order, itemResponses);
    }


    /* 임시 주문 목록 저장 메서드 */
    @Override
    public Order saveOrderToMockStorage(Order order) {
        AtomicLong orderIdCounter = new AtomicLong(0L);

        Long newId = orderIdCounter.incrementAndGet();
        order.setId(newId);
        mockOrderStorage.put(newId, order);
        return order;
    }
}