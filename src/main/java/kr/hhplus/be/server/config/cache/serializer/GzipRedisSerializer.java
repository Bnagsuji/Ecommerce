package kr.hhplus.be.server.config.cache.serializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipRedisSerializer<T> implements RedisSerializer<T> {

    private final ObjectMapper objectMapper;
    private final JavaType javaType;
    private final int minCompressSize;
    private final int bufferSize;

    private static final byte[] GZIP_MAGIC = new byte[] { (byte)0x1f, (byte)0x8b };

    public GzipRedisSerializer(ObjectMapper objectMapper,
                               TypeReference<T> typeRef,
                               int minCompressSize,
                               int bufferSize) {
        this.objectMapper = objectMapper;
        this.javaType = objectMapper.getTypeFactory().constructType(typeRef);
        this.minCompressSize = Math.max(minCompressSize, 512);
        this.bufferSize = Math.max(bufferSize, 1024);
    }

    @Override
    public byte[] serialize(T value) {
        if (value == null) return null;
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            if (json.length <= minCompressSize) {
                return json;
            }
            return compress(json);
        } catch (Exception e) {
            throw new IllegalStateException("Redis serialize error", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null) return null;
        try {
            if (isGzip(bytes)) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes), bufferSize);
                     FastBAOS out = new FastBAOS(bufferSize)) {
                    StreamUtils.copy(gis, out);
                    return objectMapper.readValue(out.getBuffer(), 0, out.size(), javaType);
                }
            } else {
                return objectMapper.readValue(bytes, javaType);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Redis deserialize error", e);
        }
    }

    private byte[] compress(byte[] src) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);
             GZIPOutputStream gos = new GZIPOutputStream(bos, bufferSize)) {
            StreamUtils.copy(src, gos);
            gos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Gzip compress error", e);
        }
    }

    private boolean isGzip(byte[] b) {
        return b.length > 2 && b[0] == GZIP_MAGIC[0] && b[1] == GZIP_MAGIC[1];
    }

    static final class FastBAOS extends ByteArrayOutputStream implements AutoCloseable {
        FastBAOS(int size) { super(size); }
        byte[] getBuffer() { return this.buf; }
        @Override public void close() {}
    }
}
