package com.mc_host.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CachingService {
    private static final Logger LOGGER = Logger.getLogger(CachingService.class.getName());

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CachingService(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> void cache(String key, T value) {
        cache(key, value, null);
    }

    public <T> void cache(String key, T value, Duration ttl) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            Optional.ofNullable(ttl)
                .ifPresentOrElse(
                    t -> redisTemplate.opsForValue().set(key, serializedValue, t),
                    () -> redisTemplate.opsForValue().set(key, serializedValue)
                );
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, String.format("Failed to serialize value for key %s", key), e);
        }
    }

    public <T> Boolean flagIfAbsent(String flag, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(flag, ", ttl");
    }

    public void evictCache(String key) {
        redisTemplate.delete(key);
    }

    public <T> Optional<T> retrieveCache(String key, Class<T> valueType) {
        String serializedValue = redisTemplate.opsForValue().get(key);
        if (serializedValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(serializedValue, valueType));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, String.format("Failed to deserialize value for key %s with type %s", key, valueType), e);
            return Optional.empty();
        }
    }

    public <T> Optional<T> retrieveCache(String key, TypeReference<T> typeReference) {
        String serializedValue = redisTemplate.opsForValue().get(key);
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
}