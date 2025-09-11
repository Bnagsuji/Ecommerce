package kr.hhplus.be.server.integration.order.kafka;

import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext
class KafkaOrderCompletedTest {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    PlatformTransactionManager txManager;

    @MockBean(name = "kafkaTemplate")
    KafkaTemplate<String, CompleteMsg> kafkaTemplate;

    @AfterEach
    void resetMocks() {
        try { reset(kafkaTemplate); } catch (Exception ignore) {}
    }

    private CompleteMsg sampleMsg() {
        return CompleteMsg.builder()
                .orderId(100L)
                .userId(200L)
                .items(java.util.List.of(new CompleteMsg.Item(1L, 2)))
                .createDate(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    void 트랜잭션_커밋시_메시지발행() {
        TransactionTemplate tt = new TransactionTemplate(txManager);

        tt.execute(status -> {
            CompleteMsg msg = sampleMsg();
            eventPublisher.publishEvent(new OrderCompletedEvent(msg, "200"));
            // 커밋 전에는 send() 호출 없음
            verify(kafkaTemplate, times(0))
                    .send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
            return null;
        });

        // 커밋 후 send() 1회 호출
        verify(kafkaTemplate, times(1))
                .send(ArgumentMatchers.anyString(), eq("200"), any(CompleteMsg.class));
    }

    @Test
    void 트랜잭션_롤백시_메시지미발행() {
        TransactionTemplate tt = new TransactionTemplate(txManager);

        tt.execute(status -> {
            CompleteMsg msg = sampleMsg();
            eventPublisher.publishEvent(new OrderCompletedEvent(msg, "200"));
            status.setRollbackOnly(); // 롤백 지정
            return null;
        });

        verify(kafkaTemplate, times(0))
                .send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void 트랜잭션없음시_메시지미발행() {
        CompleteMsg msg = sampleMsg();
        eventPublisher.publishEvent(new OrderCompletedEvent(msg, "200"));

        verify(kafkaTemplate, times(0))
                .send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }
}
