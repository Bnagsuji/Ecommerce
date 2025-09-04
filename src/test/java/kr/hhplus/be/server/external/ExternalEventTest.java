package kr.hhplus.be.server.external;

import kr.hhplus.be.server.config.event.EventConfig;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.service.external.ExternalPlatformService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(EventConfig.class)
class ExternalEventTest {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    PlatformTransactionManager manager;

    @MockBean
    ExternalPlatformService externalService;

    private CompleteMsg sampleMsg(long orderId) {
        return CompleteMsg.builder()
                .orderId(orderId)
                .userId(10L)
                .items(List.of(new CompleteMsg.Item(100L, 2)))
                .createDate(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        reset(externalService);
    }

    @Test
    void 커밋후에_외부메시지전송_확인_테스트() {
        CompleteMsg msg = sampleMsg(1L);

        new TransactionTemplate(manager).execute(status -> {
            publisher.publishEvent(msg);
            return null;
        });

        // @Async 고려해서 Awaitility 사용
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(externalService, times(1)).sendOrderCompletionData(any()));
    }


    @Test
    void 롤백되면_외부메시지_전송되지_않는다() {
        CompleteMsg msg = sampleMsg(2L);

        try {
            new TransactionTemplate(manager).execute(status -> {
                publisher.publishEvent(msg);
                // 롤백 시도
                status.setRollbackOnly();
                return null;
            });
        } catch (Exception ignored) {}

        Awaitility.await().pollDelay(Duration.ofMillis(300)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(externalService, times(0)).sendOrderCompletionData(any()));
    }


    @Test
    void 한트랜잭션에서_여러번_publish하면_모두_전송() {
        CompleteMsg a = sampleMsg(3L);
        CompleteMsg b = sampleMsg(4L);
        CompleteMsg c = sampleMsg(5L);

        new TransactionTemplate(manager).execute(status -> {
            publisher.publishEvent(a);
            publisher.publishEvent(b);
            publisher.publishEvent(c);
            return null;
        });

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(externalService, times(3)).sendOrderCompletionData(any()));
    }


    @Test
    void 트랜잭션밖_publish는_전송되지_않는다() {
        CompleteMsg msg = sampleMsg(6L);

        publisher.publishEvent(msg);

        Awaitility.await().pollDelay(Duration.ofMillis(300)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(externalService, times(0)).sendOrderCompletionData(any()));
    }

}
