package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.domain.ExternalPlatformService;
import kr.hhplus.be.server.domain.account.MockAccountService;
import kr.hhplus.be.server.domain.coupon.CouponService;
import kr.hhplus.be.server.domain.order.request.OrderRequest;
import kr.hhplus.be.server.domain.order.response.OrderResponse;
import kr.hhplus.be.server.domain.product.MockProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final MockAccountService accountService = new MockAccountService();
    private final MockProductService productService = new MockProductService();
    private final CouponService couponService = new CouponService();
    private final ExternalPlatformService externalPlatformService = new ExternalPlatformService();

    public OrderResponse processOrder(OrderRequest request) {
        Long memberId = request.getMemberId();
        List<OrderRequest.OrderItem> items = request.getItems();
        Long couponId = request.getCouponId();

        int totalPrice = 0;

        try {
            // 1. 상품 가격 계산
            for (OrderRequest.OrderItem item : items) {
                int price = productService.getProductById(item.getProductId()).price();
                totalPrice += price * item.getQuantity();
            }

            // 2. 쿠폰 적용
            if (couponId != null) {
                int discount = couponService.applyCoupon(memberId, couponId);
                totalPrice -= discount;
            }

            // 3. 잔액 확인 및 차감
            int currentBalance = accountService.getBalance(memberId).balance();
            if (currentBalance < totalPrice) {
                throw new IllegalStateException("잔액 부족");
            }
            accountService.deductBalance(memberId, totalPrice);

            // 4. 재고 차감
            for (OrderRequest.OrderItem item : items) {
                productService.decreaseStock(item.getProductId(), item.getQuantity());
            }

            // 5. 주문 및 결제 등록 (더미 처리)
            // 6. 외부 전송
            externalPlatformService.sendOrderData(request);

            // 7. 히스토리 저장 (생략 가능)

            return new OrderResponse(true, "주문 완료");

        } catch (Exception e) {
            // 롤백 처리
            accountService.rollback(memberId);
            productService.rollback(items);
            if (couponId != null) {
                couponService.rollback(memberId, couponId);
            }

            return new OrderResponse(false, "주문 실패: " + e.getMessage());
        }
    }
}