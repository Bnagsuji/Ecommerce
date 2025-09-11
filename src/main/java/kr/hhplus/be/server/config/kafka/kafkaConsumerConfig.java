package kr.hhplus.be.server.config.kafka;

import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.infrastructure.messaging.kafka.coupon.KafkaCouponIssueDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class kafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<Long, KafkaCouponIssueDTO> couponIssueConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // groupId는 @KafkaListener에서 지정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<KafkaCouponIssueDTO> valueDeserializer =
                new JsonDeserializer<>(KafkaCouponIssueDTO.class, false);
        valueDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new LongDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "couponIssueContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Long, KafkaCouponIssueDTO> couponIssueContainerFactory(
            ConsumerFactory<Long, KafkaCouponIssueDTO> cf
    ) {
        ConcurrentKafkaListenerContainerFactory<Long, KafkaCouponIssueDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }



}
