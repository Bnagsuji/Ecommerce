package kr.hhplus.be.server.service.external;

import kr.hhplus.be.server.domain.order.Order;

public interface ExternalPlatformService {
    void sendOrderCompletionData(Order order);
}
