package kr.hhplus.be.server.service.external;

import kr.hhplus.be.server.domain.order.external.CompleteMsg;

public interface ExternalPlatformService {
    void sendOrderCompletionData(CompleteMsg msg);
}
