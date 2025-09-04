package kr.hhplus.be.server.service.external.impl;

import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.service.external.ExternalPlatformService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExternalPlatformServiceImpl implements ExternalPlatformService {


    @Override
    public void sendOrderCompletionData(CompleteMsg msg) {
        sendOrderToAnalyticsPlatform(msg);
    }


    private void sendOrderToAnalyticsPlatform(CompleteMsg msg) {
        List<String> itemDetails = msg.items().stream().map(item -> item.toString()).collect(Collectors.toList());

        System.out.println("  주문 ID: " + msg.orderId());
        System.out.println("  사용자 ID: " + msg.userId());
        System.out.println("  주문 상품: " + String.join(", ", itemDetails));
        System.out.println("--------------------------------------------------");
    }


}
