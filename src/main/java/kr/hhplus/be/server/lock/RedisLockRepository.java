package kr.hhplus.be.server.lock;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisLockRepository {
    private final StringRedisTemplate redis;

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    public RedisLockRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    public boolean unlock(String key, String token) throws DataAccessException {
        byte[] k = RedisSerializer.string().serialize(key);
        byte[] s = RedisSerializer.string().serialize(UNLOCK_LUA);
        byte[] t = RedisSerializer.string().serialize(token);

        Long r = redis.execute(connection ->
                        connection.scriptingCommands().eval(
                                s,
                                ReturnType.INTEGER,
                                1,           // numKeys = 1 (k가 key)
                                k, t         // varargs: [key..., args...]
                        ),
                true,  // exposeConnection
                true   // pipeline
        );

        return r != null && r > 0;
    }
}
