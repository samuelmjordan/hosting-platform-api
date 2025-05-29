package com.mc_host.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.cache.Queue;
import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class Cache {
    private static final Logger LOGGER = Logger.getLogger(Cache.class.getName());
    private static final CacheNamespace API_NAMESPACE = CacheNamespace.API;
    private static final CacheNamespace QUEUE_NAMESPACE = CacheNamespace.QUEUE;
    private static final String DELIMITER = "::";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Cache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String composeKey(CacheNamespace namespace, String key) {
        return String.join(DELIMITER, API_NAMESPACE.name(), namespace.name(), key);
    }

    public <T> void set(CacheNamespace namespace, String key, T value) {
        set(namespace, key, value, null);
    }

    public <T> void set(CacheNamespace namespace, String key, T value, Duration ttl) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            String composedKey = composeKey(namespace, key);
            Optional.ofNullable(ttl)
                .ifPresentOrElse(
                    t -> redisTemplate.opsForValue().set(composedKey, serializedValue, t),
                    () -> redisTemplate.opsForValue().set(composedKey, serializedValue)
                );
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, String.format("Failed to serialize value for key %s", key), e);
        }
    }

    public Boolean exists(CacheNamespace namespace, String flag) {
        return redisTemplate.hasKey(composeKey(namespace, flag));
    }

    public <T> Boolean flagIfAbsent(CacheNamespace namespace, String flag, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(composeKey(namespace, flag), "", ttl);
    }

    public void evict(CacheNamespace namespace, String key) {
        redisTemplate.delete(composeKey(namespace, key));
    }

    public <T> Optional<T> retrieve(CacheNamespace namespace, String key, Class<T> valueType) {
        return retrieve(namespace, key, new TypeReference<T>() {
            @Override
            public Type getType() {
                return valueType;
            }
        });
    }

    public <T> Optional<T> retrieve(CacheNamespace namespace, String key, TypeReference<T> typeReference) {
        String serializedValue = redisTemplate.opsForValue().get(composeKey(namespace, key));
        if (serializedValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(serializedValue, typeReference));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, String.format("Failed to deserialize value for key %s with type %s", key, typeReference), e);
            return Optional.empty();
        }
    }

    public void queueLeftPush(Queue queue, String value) {
        String script = """
            local key = KEYS[1]
            local value = ARGV[1]
            local pos = redis.call('LPOS', key, value)
            if not pos then
                redis.call('LPUSH', key, value)
                return 1
            end
            return 0
            """;
        
        Long result = redisTemplate.execute(
            RedisScript.of(script, Long.class),
            List.of(composeKey(QUEUE_NAMESPACE, queue.name())),
            value
        );
        if (result == 0) {
            LOGGER.info(String.format("Duplicate push queue: %s, value: %s", queue, value));
        }
    }

    public String queueRead(Queue queue) {
        try {
            return redisTemplate.opsForList().rightPop(composeKey(QUEUE_NAMESPACE, queue.name()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to read queue %s", queue), e);
            throw e;
        }        
    }
}