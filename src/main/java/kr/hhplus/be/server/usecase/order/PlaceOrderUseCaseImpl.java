package kr.hhplus.be.server.usecase.order;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.coupon.CouponService;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final ProductService productService;
    private final AccountService accountService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;

    @Override
    @DistributedLock(
            keys = {
                    "#req.items.![ 'product:' + productId ]",    // 상품별 멀티락
                    "'account:' + #req.userId",
                    "#req.couponId != null ? 'coupon:' + #req.couponId : null"  // 쿠폰 optional 락
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

        List<ProductResponse> productResponses = handleStock(req.getItems());
        Map<Long, ProductResponse> productMap = ProductResponse.toMapById(productResponses);
        List<OrderItem> orderItems = createOrderItems(req, productMap);
        long totalAmount = Order.calculateTotalAmount(orderItems);
        long finalAmount = totalAmount;

        if (req.getCouponId() != null) {
            boolean issued = couponService.issueCoupon(req.getUserId(), req.getCouponId());
            if (!issued) {
                throw new IllegalStateException("쿠폰 발급에 실패했습니다.");
            }

            boolean used = couponService.useCoupon(req.getUserId(), req.getCouponId());
            if (!used) {
                throw new IllegalStateException("쿠폰 사용에 실패했습니다.");
            }

            finalAmount = calculateFinalAmount(req.getUserId(), req.getCouponId(), totalAmount);
        }

        try {
            handlePayment(req.getUserId(), finalAmount);
            Order order = Order.create(req, orderItems);
            Order savedOrder = orderRepository.save(order);
            return OrderResponse.from(savedOrder);
        } catch (Exception e) {
            rollbackCouponIfNecessary(req.getUserId(), req.getCouponId());
            throw e;
        }
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

    private long calculateFinalAmount(Long userId, Long couponId, long totalAmount) {
        if (couponId == null) return totalAmount;
        int discountAmount = couponService.applyCoupon(userId, couponId);
        return Math.max(totalAmount - discountAmount, 0);
    }

    private void handlePayment(Long userId, long totalAmount) {
        AccountResponse response = accountService.useBalance(userId, totalAmount);
        if (response.amount() < 100L) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
    }

    private void rollbackCouponIfNecessary(Long userId, Long couponId) {
        if (couponId == null) return;
        couponService.rollback(userId, couponId);
    }
}
