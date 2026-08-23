package com.bbangpatrol.bakery.cache;

import com.bbangpatrol.bakery.dto.AttractionListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttractionCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "ATTRACTION:";
    private static final long TTL_HOURS = 24;

    public Optional<AttractionListResponse> find(Long storeId) {
        String cached = redisTemplate.opsForValue().get(PREFIX + storeId);
        if (cached == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, AttractionListResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("[AttractionCache] 캐시 역직렬화 실패 storeId: {}", storeId, e);
            return Optional.empty();
        }
    }

    public void save(Long storeId, AttractionListResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(PREFIX + storeId, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("[AttractionCache] 캐시 직렬화 실패 storeId: {}", storeId, e);
        }
    }
}
