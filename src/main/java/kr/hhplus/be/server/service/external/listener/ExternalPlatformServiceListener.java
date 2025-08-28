package kr.hhplus.be.server.service.external.listener;

import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.service.external.ExternalPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ExternalPlatformServiceListener {


    private final ExternalPlatformService externalService;



    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void completeOrder(CompleteMsg msg) {
        externalService.sendOrderCompletionData(msg);
    }






}
