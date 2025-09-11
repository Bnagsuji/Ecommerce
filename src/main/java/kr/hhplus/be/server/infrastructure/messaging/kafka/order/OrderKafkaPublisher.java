package kr.hhplus.be.server.infrastructure.messaging.kafka.order;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 임시 카프카 퍼블리셔 */
@Component
@RequiredArgsConstructor
public class OrderKafkaPublisher {


    private final KafkaTemplate<String, CompleteMsg> kafkaTemplate;


    public void publishAfterCommit(String key, CompleteMsg payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            kafkaTemplate.send(KafkaProducerConfig.ORDER_TOPIC, key, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(KafkaProducerConfig.ORDER_TOPIC, key, payload);
            }
        });
    }
}
