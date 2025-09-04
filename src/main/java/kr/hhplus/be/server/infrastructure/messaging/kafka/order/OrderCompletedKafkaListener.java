package kr.hhplus.be.server.infrastructure.messaging.kafka.order;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderCompletedKafkaListener {

    private final KafkaTemplate<String, CompleteMsg> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        kafkaTemplate.send(KafkaProducerConfig.ORDER_TOPIC, event.key(), event.payload());
    }

}
