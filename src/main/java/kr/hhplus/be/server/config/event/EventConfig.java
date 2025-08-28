package kr.hhplus.be.server.config.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

@Configuration
@EnableAsync
public class EventConfig {


    @Bean
    public TransactionalEventListenerFactory transactionalEventListenerFactory() {
        return new TransactionalEventListenerFactory();
    }
}