package kr.hhplus.be.server.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.config.cache.serializer.GzipRedisSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    @Bean
    public CacheManager redisCacheManager() {
        Map<String, RedisCacheConfiguration> initial =
                Arrays.stream(RedisCache.values())
                        .collect(Collectors.toMap(
                                RedisCache::getCacheName,
                                cfg -> RedisCacheConfiguration.defaultCacheConfig()
                                        .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                                        )
                                        .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                        new GzipRedisSerializer<>(
                                                                objectMapper, cfg.getTypeRef(),
                                                                2 * 1024,
                                                                4 * 1024
                                                        )
                                                )
                                        )
                                        .disableCachingNullValues()
                                        .entryTtl(cfg.getExpiredAfterWrite())
                                        .prefixCacheNameWith("topSelling:")
                        ));

        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(initial)
                .build();
    }
}
