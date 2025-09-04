package kr.hhplus.be.server.infrastructure.messaging.kafka.coupon;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueKafkaListener {

    private final CouponJpaRepository couponRepository;
    private final UserCouponJpaRepository userCouponRepository;


    @Transactional
    @KafkaListener(
            topics = KafkaProducerConfig.COUPON_REQ_TOPIC,
            groupId = "coupon-issuer",
            containerFactory = "couponIssueContainerFactory"
    )
    public void onIssue(ConsumerRecord<Long, KafkaCouponIssueDTO> record, Acknowledgment ack) {
        Long couponId = record.key();
        KafkaCouponIssueDTO dto = record.value();

        try {
            int updated = couponRepository.decreaseIfAvailable(couponId);
            if (updated == 0) {
                log.info("[coupon-issue] sold out or inactive: coupon={}", couponId);
                ack.acknowledge();
                return;
            }

            UserCoupon entity = UserCoupon.builder()
                    .userId(dto.userId())
                    .coupon(couponRepository.getReferenceById(couponId))
                    .build();
            userCouponRepository.save(entity);

            ack.acknowledge();
            log.info("[coupon-issue] issued: user={} coupon={} offset={}", dto.userId(), couponId, record.offset());

        } catch (DataIntegrityViolationException dup) {
            couponRepository.increase(couponId);
            ack.acknowledge();
            log.info("[coupon-issue] duplicate grant ignored: user={} coupon={}", dto.userId(), couponId);

        } catch (RuntimeException ex) {
            log.error("[coupon-issue] failed, will retry: user={} coupon={}", dto.userId(), couponId, ex);
            throw ex;
        }
    }
}
