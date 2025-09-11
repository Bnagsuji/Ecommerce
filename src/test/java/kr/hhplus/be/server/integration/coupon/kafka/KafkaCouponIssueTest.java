package kr.hhplus.be.server.integration.coupon.kafka;

import kr.hhplus.be.server.config.kafka.KafkaProducerConfig;
import kr.hhplus.be.server.infrastructure.messaging.kafka.coupon.KafkaCouponIssueDTO;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.service.coupon.impl.CouponServiceImpl;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                KafkaProducerConfig.class,
                CouponServiceImpl.class
        }
)
@ActiveProfiles("test")
public class KafkaCouponIssueTest {

        private static final KafkaContainer KAFKA =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

        @BeforeAll
        static void startKafkaAndCreateTopic() throws Exception {
                KAFKA.start();

                try (AdminClient admin = AdminClient.create(Map.of(
                        org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
                ))) {
                        admin.createTopics(List.of(
                                new NewTopic(KafkaProducerConfig.COUPON_REQ_TOPIC, 1, (short) 1) // "coupon-publish-request"
                        )).all().get();
                }
        }

        @AfterAll
        static void stopKafka() {
                KAFKA.stop();
        }

        @DynamicPropertySource
        static void kafkaProps(DynamicPropertyRegistry registry) {
                registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        }

        @MockBean private CouponJpaRepository couponJpaRepository;
        @MockBean private UserCouponJpaRepository userCouponJpaRepository;

        @Autowired
        private CouponServiceImpl couponService;

        @Test
        void issueCoupon_메시지가_토픽으로_발행된다() {
                // given
                Long userId = 1L;
                Long couponId = 100L;

                // when
                boolean accepted = couponService.issueCoupon(userId, couponId);

                // then
                assertThat(accepted).isTrue();

                KafkaCouponIssueDTO dto = pollOneRecordValue(
                        KafkaProducerConfig.COUPON_REQ_TOPIC,
                        couponId
                );

                assertThat(dto).isNotNull();
                assertThat(dto.userId()).isEqualTo(userId);
                assertThat(dto.couponId()).isEqualTo(couponId);
        }

        @Test
        void 같은_쿠폰ID_여러_요청시_소비_순서가_발행_순서를_유지한다() {
                // given: 같은 쿠폰, 서로 다른 사용자 순서대로
                Long couponId = 777L;
                List<Long> userIdsInOrder = List.of(10L, 11L, 12L, 13L, 14L);

                // when
                for (Long uid : userIdsInOrder) {
                        boolean ok = couponService.issueCoupon(uid, couponId);
                        assertThat(ok).isTrue();
                }

                // then: 소비된 userId 순서가 발행 순서와 동일해야 함
                List<KafkaCouponIssueDTO> received = pollNRecordsValues(
                        KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, userIdsInOrder.size()
                );

                List<Long> receivedUserIds = received.stream().map(KafkaCouponIssueDTO::userId).toList();
                assertThat(receivedUserIds).containsExactlyElementsOf(userIdsInOrder);
        }

        @Test
        void 배치_발행시_요청_개수와_수신_개수_일치() {
                // given
                Long couponId = 2025L;
                List<Long> userIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);

                // when
                for (Long uid : userIds) {
                        assertThat(couponService.issueCoupon(uid, couponId)).isTrue();
                }

                // then
                List<KafkaCouponIssueDTO> received = pollNRecordsValues(
                        KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, userIds.size()
                );
                assertThat(received).hasSize(userIds.size());
                received.forEach(dto -> {
                        assertThat(dto.couponId()).isEqualTo(couponId);
                        assertThat(userIds).contains(dto.userId());
                });
        }

        @Test
        void 서로_다른_컨슈머그룹은_동일_메시지를_각자_소비한다() {
                // given
                Long userId = 99L;
                Long couponId = 3030L;
                assertThat(couponService.issueCoupon(userId, couponId)).isTrue();

                // when
                KafkaCouponIssueDTO a = pollOneRecordValueWithGroup(
                        KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, "group-A-" + UUID.randomUUID()
                );
                KafkaCouponIssueDTO b = pollOneRecordValueWithGroup(
                        KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, "group-B-" + UUID.randomUUID()
                );

                // then
                assertThat(a).isNotNull();
                assertThat(b).isNotNull();
                assertThat(a.userId()).isEqualTo(userId);
                assertThat(b.userId()).isEqualTo(userId);
                assertThat(a.couponId()).isEqualTo(couponId);
                assertThat(b.couponId()).isEqualTo(couponId);
        }

        private KafkaCouponIssueDTO pollOneRecordValue(String topic, Long expectedKey) {
                JsonDeserializer<KafkaCouponIssueDTO> valueDeserializer =
                        new JsonDeserializer<>(KafkaCouponIssueDTO.class, false);
                valueDeserializer.addTrustedPackages("*");

                DefaultKafkaConsumerFactory<Long, KafkaCouponIssueDTO> cf =
                        new DefaultKafkaConsumerFactory<>(
                                Map.of(
                                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                                        ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
                                ),
                                new LongDeserializer(),
                                valueDeserializer
                        );

                try (Consumer<Long, KafkaCouponIssueDTO> consumer = cf.createConsumer()) {
                        consumer.subscribe(List.of(topic));

                        long deadline = System.currentTimeMillis() + 5_000L;
                        while (System.currentTimeMillis() < deadline) {
                                ConsumerRecords<Long, KafkaCouponIssueDTO> records =
                                        consumer.poll(Duration.ofMillis(200));
                                for (ConsumerRecord<Long, KafkaCouponIssueDTO> record : records) {
                                        if (record.key() != null && record.key().equals(expectedKey)) {
                                                return record.value();
                                        }
                                }
                        }
                }
                return null;
        }

        private List<KafkaCouponIssueDTO> pollNRecordsValues(String topic, Long expectedKey, int n) {
                JsonDeserializer<KafkaCouponIssueDTO> valueDeserializer =
                        new JsonDeserializer<>(KafkaCouponIssueDTO.class, false);
                valueDeserializer.addTrustedPackages("*");

                DefaultKafkaConsumerFactory<Long, KafkaCouponIssueDTO> cf =
                        new DefaultKafkaConsumerFactory<>(
                                Map.of(
                                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                                        ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
                                ),
                                new LongDeserializer(),
                                valueDeserializer
                        );

                try (Consumer<Long, KafkaCouponIssueDTO> consumer = cf.createConsumer()) {
                        consumer.subscribe(List.of(topic));

                        long deadline = System.currentTimeMillis() + 10_000L;
                        ArrayList<KafkaCouponIssueDTO> results = new ArrayList<>();

                        while (System.currentTimeMillis() < deadline && results.size() < n) {
                                ConsumerRecords<Long, KafkaCouponIssueDTO> records = consumer.poll(Duration.ofMillis(200));
                                for (ConsumerRecord<Long, KafkaCouponIssueDTO> rec : records) {
                                        if (rec.key() != null && rec.key().equals(expectedKey)) {
                                                results.add(rec.value());
                                                if (results.size() == n) break;
                                        }
                                }
                        }
                        return results;
                }
        }

        private KafkaCouponIssueDTO pollOneRecordValueWithGroup(String topic, Long expectedKey, String groupId) {
                JsonDeserializer<KafkaCouponIssueDTO> valueDeserializer =
                        new JsonDeserializer<>(KafkaCouponIssueDTO.class, false);
                valueDeserializer.addTrustedPackages("*");

                DefaultKafkaConsumerFactory<Long, KafkaCouponIssueDTO> cf =
                        new DefaultKafkaConsumerFactory<>(
                                Map.of(
                                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                                        ConsumerConfig.GROUP_ID_CONFIG, groupId,
                                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
                                ),
                                new LongDeserializer(),
                                valueDeserializer
                        );

                try (Consumer<Long, KafkaCouponIssueDTO> consumer = cf.createConsumer()) {
                        consumer.subscribe(List.of(topic));
                        long deadline = System.currentTimeMillis() + 5_000L;
                        while (System.currentTimeMillis() < deadline) {
                                ConsumerRecords<Long, KafkaCouponIssueDTO> records = consumer.poll(Duration.ofMillis(200));
                                for (ConsumerRecord<Long, KafkaCouponIssueDTO> record : records) {
                                        if (record.key() != null && record.key().equals(expectedKey)) {
                                                return record.value();
                                        }
                                }
                        }
                        return null;
                }
        }
}
