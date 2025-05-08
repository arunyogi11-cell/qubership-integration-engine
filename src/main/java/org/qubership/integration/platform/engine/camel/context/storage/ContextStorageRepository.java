package org.qubership.integration.platform.engine.camel.context.storage;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Slf4j
public class ContextStorageRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ValueOperations<String, String> valueOperations;

    public ContextStorageRepository(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper
             ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueOperations = redisTemplate.opsForValue();
    }


    public void add(String key, String value) {
        valueOperations.set(key, value);
    }

    public String get(String key) {
        return valueOperations.get(key);
    }

    public String delete(String key) {
        return valueOperations.getAndDelete(key);
    }


}

