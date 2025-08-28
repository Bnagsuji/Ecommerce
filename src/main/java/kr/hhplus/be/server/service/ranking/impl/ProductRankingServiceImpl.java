package kr.hhplus.be.server.service.ranking.impl;

import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.service.ranking.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RKeysAsync;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RScriptAsync;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingServiceImpl implements ProductRankingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    // ex) rank:prod:20250819
    private static final String DAY_KEY_PREFIX = "rank:prod:";
    // ex) rank:prod:tmp:5d:uuid
    private static final String TMP_KEY_PREFIX = "rank:prod:tmp:";

    private final RedissonClient redisson;
    private final ProductRepository productRepository;

    // 주문 수량 증가: 오늘 날짜 키에 productId score += quantity
    @Override
    public void incrOrder(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String todayKey = dayKey(LocalDate.now(ZONE));
        RBatch batch = redisson.createBatch();

        for (OrderItem item : items) {
            long productId = item.getProductId();
            long qty = item.getQuantity();

            RScoredSortedSetAsync<String> zset = batch.getScoredSortedSet(todayKey);
            zset.addScoreAsync(String.valueOf(productId), qty);
        }

        RKeysAsync keys = batch.getKeys();
        keys.expireAsync(todayKey, 1, TimeUnit.DAYS);

        batch.execute();
    }

    @Override
    public List<ProductResponse> getTopForRecentDaysPipelined(int days, int limit) {
        if (days <= 0 || limit <= 0) {
            return List.of();
        }

        List<String> dayKeys = collectDayKeys(days);
        if (dayKeys.isEmpty()) {
            return List.of();
        }

        String tmpKey = TMP_KEY_PREFIX + days + "d:" + UUID.randomUUID();

        RBatch batch = redisson.createBatch();


        List<Object> luaKeys = buildZunionstoreKeys(tmpKey, dayKeys);

        RScriptAsync script = batch.getScript();
        script.evalAsync(
                org.redisson.api.RScript.Mode.READ_WRITE,
                // KEYS = [dest, dayKey1, dayKey2, ...]
                "local dest = KEYS[1]\n" +
                        "local num = #KEYS - 1\n" +
                        "local cmd = {'ZUNIONSTORE', dest, tostring(num)}\n" +
                        "for i = 2, #KEYS do table.insert(cmd, KEYS[i]) end\n" +
                        "table.insert(cmd, 'WEIGHTS')\n" +
                        "for i = 1, num do table.insert(cmd, '1') end\n" +
                        "table.insert(cmd, 'AGGREGATE')\n" +
                        "table.insert(cmd, 'SUM')\n" +
                        "return redis.call(unpack(cmd))\n",
                org.redisson.api.RScript.ReturnType.INTEGER,
                luaKeys
        );

        // 2) 임시키 TTL 30초
        RKeysAsync keys = batch.getKeys();
        keys.expireAsync(tmpKey, 30, TimeUnit.SECONDS);

        // 3) TOP N 멤버 조회 (내림차순)
        RScoredSortedSetAsync<String> tmpZset = batch.getScoredSortedSet(tmpKey);
        RFuture<Collection<String>> topFuture =
                tmpZset.valueRangeReversedAsync(0, Math.max(0, limit - 1));

        // 4) 파이프라인 실행
        BatchResult<?> result = batch.execute();

        // 5) 결과 파싱
        Collection<String> topProductIdStrs = topFuture.getNow();
        if (topProductIdStrs == null || topProductIdStrs.isEmpty()) {
            return List.of();
        }

        List<Long> topIdsInRankOrder = topProductIdStrs.stream()
                .map(Long::valueOf)
                .toList();

        List<Product> products = productRepository.findAllById(topIdsInRankOrder);
        Map<Long, Product> byId = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductResponse> responses = new ArrayList<ProductResponse>(topIdsInRankOrder.size());
        for (Long id : topIdsInRankOrder) {
            Product p = byId.get(id);
            if (p != null) {
                responses.add(ProductResponse.from(p)); // ← 중복 추가 제거
            }
        }
        return responses;
    }

    private static String dayKey(LocalDate day) {
        return DAY_KEY_PREFIX + DAY_FMT.format(day);
    }

    private List<String> collectDayKeys(int days) {
        LocalDate today = LocalDate.now(ZONE);
        List<String> keys = new ArrayList<String>(days);
        for (int i = 0; i < days; i++) {
            keys.add(dayKey(today.minusDays(i)));
        }
        return keys;
    }


    private static List<Object> buildZunionstoreKeys(String destKey, List<String> dayKeys) {
        List<Object> keys = new ArrayList<Object>(1 + dayKeys.size());
        keys.add(destKey);
        keys.addAll(dayKeys);
        return keys;
    }
}
