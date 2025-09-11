package kr.hhplus.be.server.infrastructure.messaging.kafka.coupon;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final KafkaTemplate<Long, KafkaCouponIssueDTO> kafkaTemplate;

    public void create(Long couponId,Long userId) {
        KafkaCouponIssueDTO dto = new KafkaCouponIssueDTO(userId, couponId,
              System.currentTimeMillis());
        kafkaTemplate.send(KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, dto);
    }




}
