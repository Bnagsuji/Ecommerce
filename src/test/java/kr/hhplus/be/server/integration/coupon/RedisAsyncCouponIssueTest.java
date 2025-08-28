package kr.hhplus.be.server.integration.coupon;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.controller.coupon.response.IssueCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.lock.key.RedisKeys;
import kr.hhplus.be.server.service.coupon.RedisCouponService;
import kr.hhplus.be.server.service.coupon.impl.CouponIssueConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(OrderAnnotation.class)
class RedisAsyncCouponIssueTest {

    @Autowired private RedisCouponService redisCouponService;
    @Autowired private CouponJpaRepository couponRepo;
    @Autowired private UserCouponJpaRepository userCouponRepo;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private CouponIssueConsumer consumer;

    private Long couponId;
    private int stock;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 생성 (유효기간: 지금부터 +2시간, 재고: 50)
        LocalDateTime now = LocalDateTime.now();
        stock = 50;

        Coupon c = new Coupon(
                "테스트쿠폰",
                now.minusMinutes(1),
                now.plusHours(2),
                stock,
                1000
        );

        c = couponRepo.save(c);
        couponId = c.getId();

        redisCouponService.ensureCouponLoaded(couponId);

    }

    @AfterEach
    void tearDown() {
        userCouponRepo.deleteAll();
        couponRepo.deleteAll();
        var conn = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection();
        conn.serverCommands().flushAll();
        conn.close();
    }

    @Test
    @Order(1)
    void 동시_요청_선착순_발급_성공_및_정합성() throws Exception {
        int totalRequests = stock + 40;
        List<Long> userIds = IntStream.rangeClosed(1, totalRequests)
                .mapToObj(i -> (long) i)
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<Future<IssueCouponResponse>> futures = new ArrayList<>(totalRequests);

        for (Long userId : userIds) {
            futures.add(pool.submit(() -> {
                try {
                    return redisCouponService.issueAsync(couponId, userId);
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(20, TimeUnit.SECONDS);
        pool.shutdown();

        long accepted = 0, soldOut = 0, duplicate = 0, notActive = 0, notFound = 0;

        for (Future<IssueCouponResponse> f : futures) {
            IssueCouponResponse r = f.get(5, TimeUnit.SECONDS);
            String status = r.getStatus() == null ? "" : r.getStatus();
            switch (status) {
                case "ACCEPTED" -> accepted++;
                case "SOLD_OUT" -> soldOut++;
                case "DUPLICATE" -> duplicate++;
                case "NOT_ACTIVE" -> notActive++;
                case "NOT_FOUND" -> notFound++;
                default -> { if (r.isSuccess()) accepted++; else soldOut++; }
            }
        }

        // 1) 선착순: ACCEPTED == stock
        assertThat(accepted).isEqualTo(stock);
        // 2) 전체 요청 수 체크
        assertThat(accepted + soldOut + duplicate + notActive + notFound).isEqualTo(totalRequests);

        long finalAccepted = accepted;
        awaitUntil(() -> {
            for (int i = 0; i < 100; i++) consumer.consume();
            return userCouponRepo.count() == finalAccepted;
        }, 10_000);

        // 4) DB 발급 수 == ACCEPTED
        assertThat(userCouponRepo.count()).isEqualTo(accepted);

        // 5) (userId, couponId) 저장 확인
        long distinct = userCouponRepo.findAll().stream()
                .map(uc -> uc.getUserId() + ":" + uc.getCoupon().getId())
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(accepted);

        // 6) Redis 재고 0
        String couponKey = RedisKeys.couponHash(couponId);
        HashOperations<String, Object, Object> h = stringRedisTemplate.opsForHash();
        String remainingStr = (String) h.get(couponKey, "stock");
        long remaining = remainingStr == null ? -999 : Long.parseLong(remainingStr);
        assertThat(remaining).isEqualTo(0);

        // 7) 큐 비어 있음
        Long qlen = stringRedisTemplate.opsForList().size(RedisKeys.issueQueue());
        assertThat(qlen == null ? 0 : qlen).isEqualTo(0L);
    }

    private static void awaitUntil(Callable<Boolean> cond, long timeoutMillis) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (Boolean.TRUE.equals(cond.call())) return;
            Thread.sleep(100);
        }
        throw new AssertionError("조건충족불가" + timeoutMillis + " ms");
    }
}
