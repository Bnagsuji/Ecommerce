package kr.hhplus.be.server.config.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum RedisCache {

    TOP_SELLING_V2(
            RedisCacheName.TOP_SELLING_V2,
            Duration.ofSeconds(30),
            new TypeReference<List<ProductResponse>>() {}
    );

    private final String cacheName;
    private final Duration expiredAfterWrite;
    private final TypeReference<?> typeRef;
}