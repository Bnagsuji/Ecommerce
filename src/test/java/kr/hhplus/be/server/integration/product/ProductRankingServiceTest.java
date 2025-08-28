package kr.hhplus.be.server.integration.product;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.service.ranking.ProductRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class ProductRankingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DAY_KEY_PREFIX = "rank:prod:";
    private static final String TMP_KEY_PREFIX = "rank:prod:tmp:";


    @Autowired
    ProductRankingService productRankingService;

    @Autowired
    RedissonClient redissonClient;

    @MockBean
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // 이전 키 정리
        RKeys keys = redissonClient.getKeys();
        for (String k : keys.getKeysByPattern(DAY_KEY_PREFIX + "*")) keys.delete(k);
        for (String k : keys.getKeysByPattern(TMP_KEY_PREFIX + "*")) keys.delete(k);

        // 오늘/어제 점수 적재
        String todayKey = dayKey(LocalDate.now(ZONE));
        String yesterdayKey = dayKey(LocalDate.now(ZONE).minusDays(1));

        RScoredSortedSet<String> today = redissonClient.getScoredSortedSet(todayKey);
        RScoredSortedSet<String> yday  = redissonClient.getScoredSortedSet(yesterdayKey);

        // 오늘
        today.addScore("1", 10);  // p1: +10
        today.addScore("2", 20);  // p2: +20
        today.addScore("3", 15);  // p3: +15
        // 어제
        yday.addScore("1", 5);    // p1: +5  -> 총 15
        yday.addScore("2", 25);   // p2: +25 -> 총 45 (1위)
        yday.addScore("3", 10);   // p3: +10 -> 총 25 (2위)

        keys.expire(todayKey, 30, TimeUnit.DAYS);
        keys.expire(yesterdayKey, 30, TimeUnit.DAYS);
    }

    @Test
    void 최근2일_TOP3_조회_및_임시키_TTL_검증() {
        // given
        int days = 2;
        int limit = 3;

        // ProductRepository mock: id -> Product 매핑
        List<Product> all = Arrays.asList(
                productWithId(1L, "p1"),
                productWithId(2L, "p2"),
                productWithId(3L, "p3")
        );
        when(productRepository.findAllById(any()))
                .thenAnswer(inv -> {
                    Set<Long> want = new HashSet<>();
                    for (Long id : (Iterable<Long>) inv.getArgument(0)) want.add(id);
                    List<Product> filtered = new ArrayList<>();
                    for (Product p : all) if (want.contains(p.getId())) filtered.add(p);
                    return filtered;
                });

        // when
        List<ProductResponse> top = productRankingService.getTopForRecentDaysPipelined(days, limit);

        // then: 순위 p2 -> p3 -> p1
        assertThat(top).extracting(ProductResponse::id)
                .containsExactly(2L, 3L, 1L);

        // then: 임시 키 TTL > 0
        String tmpPrefix = TMP_KEY_PREFIX + days + "d:";
        String tmpKey = null;
        for (String k : redissonClient.getKeys().getKeysByPattern(tmpPrefix + "*")) {
            tmpKey = k;
            break;
        }
        assertThat(tmpKey).as("임시 키가 생성되어야 함").isNotNull();
        Long ttl = redissonClient.getKeys().remainTimeToLive(tmpKey);
        assertThat(ttl).as("임시 키 TTL이 0보다 커야 함").isNotNull();
        assertThat(ttl).isGreaterThan(0L);
    }



    @Test
    void 최근1일_TOP2_조회() {
        // given
        int days = 1;
        int limit = 2;

        // ProductRepository mock
        List<Product> all = Arrays.asList(
                productWithId(1L, "p1"),
                productWithId(2L, "p2"),
                productWithId(3L, "p3")
        );
        when(productRepository.findAllById(any()))
                .thenAnswer(inv -> {
                    Set<Long> want = new HashSet<>();
                    for (Long id : (Iterable<Long>) inv.getArgument(0)) want.add(id);
                    List<Product> filtered = new ArrayList<>();
                    for (Product p : all) if (want.contains(p.getId())) filtered.add(p);
                    return filtered;
                });

        // when
        List<ProductResponse> top = productRankingService.getTopForRecentDaysPipelined(days, limit);

        // then: 오늘 점수만 기준 → p2(20) > p3(15) > p1(10)
        assertThat(top).extracting(ProductResponse::id)
                .containsExactly(2L, 3L); // limit=2 → p2, p3
    }

    @Test
    void 조회기간내_데이터없으면_빈리스트반환() {
        // given
        int days = 5; // 5일 전부터 오늘까지, but 세팅은 오늘/어제만 넣음
        int limit = 3;

        when(productRepository.findAllById(any())).thenReturn(Collections.emptyList());

        // Redis에 있는 건 오늘/어제만 -> 5일 조회 시 실제 합산키 없음
        for (String k : redissonClient.getKeys().getKeysByPattern("rank:prod:tmp:*")) {
            redissonClient.getKeys().delete(k);
        }

        // when
        List<ProductResponse> top = productRankingService.getTopForRecentDaysPipelined(days, limit);

        // then
        assertThat(top).isEmpty();
    }

    private static String dayKey(LocalDate day) {
        return DAY_KEY_PREFIX + DAY_FMT.format(day);
    }

    private static Product productWithId(Long id, String name) {
        Product p = Product.builder()
                .name(name)
                .stock(0)
                .price(0L)
                .regDate(LocalDateTime.now())
                .build();
        setField(p, "id", id);
        return p;
    }

    private static void setField(Object target, String field, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 필드 세팅 실패: " + field, e);
        }
    }
}
