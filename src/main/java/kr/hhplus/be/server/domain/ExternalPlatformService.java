package kr.hhplus.be.server.domain;

import kr.hhplus.be.server.domain.order.request.OrderRequest;
import org.springframework.stereotype.Service;

@Service
public class ExternalPlatformService {

    public void sendOrderData(OrderRequest request) {
        // 외부 플랫폼 연동 더미

        System.out.println("외부 플랫폼 전송: 주문 정보 = " + request);
    }
}
