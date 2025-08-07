package kr.hhplus.be.server.service.external.impl;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.service.external.ExternalPlatformService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExternalPlatformServiceImpl implements ExternalPlatformService {

    @Override
    public void sendOrderCompletionData(Order order) {
        sendOrderToAnalyticsPlatform(order);
        sendOrderCompletionNotification(order);
    }


    private void sendOrderToAnalyticsPlatform(Order order) {
        List<String> itemDetails = order.getOrderItems().stream()
                .map(item -> item.getProductId() + " (수량: " + item.getQuantity() + ", 총액: " + item.getTotalAmount() + "원)")
                .collect(Collectors.toList());

        System.out.println("  주문 ID: " + order.getId());
        System.out.println("  사용자 ID: " + order.getUserId());
        System.out.println("  총 결제 금액: " + order.getTotalAmount() + "원");
        System.out.println("  주문 일시: " + order.getOrderDate());
        System.out.println("  주문 상품: " + String.join(", ", itemDetails));
        System.out.println("--------------------------------------------------");
    }

    private void sendOrderCompletionNotification(Order order) {
        String userNotificationMessage = String.format(
                "주문완료    :",
                order.getId(),
                order.getTotalAmount(),
                order.getOrderDate().toLocalDate().toString(),
                order.getOrderItems().stream()
                        .map(item -> item.getProductId() + " (" + item.getQuantity() + "개)")
                        .collect(Collectors.joining(", "))
        );

    }

}
