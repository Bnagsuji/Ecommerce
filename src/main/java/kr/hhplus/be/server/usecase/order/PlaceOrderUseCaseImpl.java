package kr.hhplus.be.server.usecase.order;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.coupon.CouponService;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final ProductService productService;
    private final AccountService accountService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @DistributedLock(
            keys = {
                    "#req.items.![ 'product:' + productId ]",           // 상품별 멀티락
                    "'account:' + #req.userId",
                    "#req.couponId != null ? 'coupon:' + #req.userId + ':' + #req.couponId : null"
            },
            prefix = "lock:",
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 2,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 100
    )
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderResponse placeOrder(OrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        // 재고 차감
        List<ProductResponse> productResponses = handleStock(req.getItems());
        Map<Long, ProductResponse> productMap = ProductResponse.toMapById(productResponses);

        // 주문 목록 생성
        List<OrderItem> orderItems = createOrderItems(req, productMap);

        // 총액/할인 계산 (주문 시점엔 '이미 발급된 쿠폰'만 사용)
        long totalAmount = Order.calculateTotalAmount(orderItems);
        int discountAmount = 0;
        if (req.getCouponId() != null) {
            discountAmount = couponService.applyCoupon(req.getUserId(), req.getCouponId());
            if (discountAmount <= 0) {
                throw new IllegalStateException("유효하지 않거나 미발급된 쿠폰입니다.");
            }
        }
        long finalAmount = Math.max(totalAmount - discountAmount, 0);

        // 결제 → 주문 저장 → 쿠폰 사용(있으면) : 같은 트랜잭션에서 처리
        handlePayment(req.getUserId(), finalAmount);

        Order order = Order.create(req, orderItems);
        Order savedOrder = orderRepository.save(order);

        if (req.getCouponId() != null) {
            boolean used = couponService.useCoupon(req.getUserId(), req.getCouponId());
            if (!used) {
                // 동시성 등으로 사용 실패 시 전체 롤백
                throw new IllegalStateException("쿠폰 사용에 실패했습니다.");
            }
        }

        // 커밋 후 발행될 도메인 이벤트 (AFTER_COMMIT 리스너가 Kafka로 전송)
        List<CompleteMsg.Item> eventItems = savedOrder.getOrderItems().stream()
                .map(i -> new CompleteMsg.Item(i.getProductId(), i.getQuantity()))
                .toList();

        CompleteMsg msg = CompleteMsg.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .items(eventItems)
                .createDate(savedOrder.getOrderDate())
                .build();

        publisher.publishEvent(new OrderCompletedEvent(msg, String.valueOf(req.getUserId())));

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
