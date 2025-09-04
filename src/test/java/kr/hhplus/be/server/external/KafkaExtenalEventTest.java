package kr.hhplus.be.server.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.external.CompleteMsg;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.coupon.CouponService;
import kr.hhplus.be.server.service.product.ProductService;
import kr.hhplus.be.server.usecase.order.PlaceOrderUseCaseImpl;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mysema.commons.lang.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
public class KafkaExtenalEventTest {
    private static final String TOPIC = "order.completed.v1";
/*
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.3")
    );

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // spring-kafka 기본 부트스트랩 서버를 Testcontainers로 대체
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // 혹시 퍼블리셔가 yml로 토픽명을 받는다면 여기서도 세팅
        // registry.add("app.kafka.order-completed-topic", () -> TOPIC);
    }

    @Autowired
    PlaceOrderUseCaseImpl useCase; // 실제 대상

    @MockBean
    ProductService productService;

    @MockBean
    AccountService accountService;

    @MockBean
    CouponService couponService;

    @MockBean
    OrderRepository orderRepository;

    private KafkaConsumer<String, String> consumer;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps(
                "ecommerce-test-consumer", "true", KAFKA.getBootstrapServers()));
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) consumer.close();
    }

    @Test
    void placeOrder_커밋후_카프카에_메시지가_적재된다() throws Exception {
        // given
        Long userId = 100L;
        Long productId = 10L;

        // 1) 상품 재고 차감 응답 목킹
        ProductResponse p = new ProductResponse(productId, "테스트상품", 1000, 1000L, LocalDateTime.now());
        when(productService.useProduct(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<ProductStockRequest> reqs = inv.getArgument(0);
            // 필요시 reqs 검증 가능
            return List.of(p);
        });

        // 2) 계좌 사용 목킹
        when(accountService.useBalance(eq(userId), anyLong()))
                .thenReturn(new kr.hhplus.be.server.controller.account.response.AccountResponse(userId, 10_000L));

        // 3) 쿠폰 미사용 시나리오
        when(couponService.issueCoupon(anyLong(), anyLong())).thenReturn(true);
        when(couponService.useCoupon(anyLong(), anyLong())).thenReturn(true);
        when(couponService.applyCoupon(anyLong(), anyLong())).thenReturn(0);

        // 4) 주문 저장 목킹 (ID, 날짜 등 세팅)
        OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .price(1000L)
                .quantity(2)
                .build();

        Order fakeSaved = Order.builder()
                .id(999L)
                .userId(userId)
                .orderItems(List.of(orderItem))
                .orderDate(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        when(orderRepository.save(Mockito.any(Order.class))).thenReturn(fakeSaved);

        // when
        OrderRequest req = new OrderRequest();
        req. (userId);
        req.setCouponId(null); // 쿠폰 없이
        req.setItems(List.of(new OrderRequest.OrderItem(productId, 2)));

        useCase.placeOrder(req); // @Transactional 메서드 → 커밋 후 퍼블리시

        // then: 카프카에서 실제 메시지를 받는다
        // 폴링 여유를 넉넉히 준다 (최대 5~10초 정도)
        CompleteMsg found = null;
        String foundKey = null;

        long deadlineMs = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadlineMs) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> r : records) {
                // key는 userId 문자열, value는 JSON(가정) → 파싱
                foundKey = r.key();
                String valueJson = r.value();
                try {
                    CompleteMsg msg = om.readValue(valueJson, CompleteMsg.class);
                    found = msg;
                    break;
                } catch (Exception parseIgnore) {
                    // 퍼블리셔가 JSON이 아닌 Serializer를 쓰면, 여기서 파서/Deserializer를 바꿔라
                }
            }
            if (found != null) break;
        }

        assertThat(found).as("카프카에서 메시지를 받아야 함").isNotNull();
        assertThat(foundKey).isEqualTo(String.valueOf(userId));
        assertThat(found.getOrderId()).isEqualTo(999L);
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(found.getItems().get(0).getQuantity()).isEqualTo(2);
    }*/
}
