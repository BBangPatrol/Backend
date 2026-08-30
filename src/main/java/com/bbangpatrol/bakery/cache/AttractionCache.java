package com.bbangpatrol.bakery.cache;

import com.bbangpatrol.bakery.dto.AttractionListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final int COORD_SCALE = 7;

    public Optional<AttractionListResponse> find(Long storeId, BigDecimal lat, BigDecimal lng) {
        String cached = redisTemplate.opsForValue().get(key(storeId, lat, lng));
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

    public void save(Long storeId, BigDecimal lat, BigDecimal lng, AttractionListResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key(storeId, lat, lng), json, TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("[AttractionCache] 캐시 직렬화 실패 storeId: {}", storeId, e);
        }
    }

    private String key(Long storeId, BigDecimal lat, BigDecimal lng) {
        return PREFIX + storeId + ":" + coord(lat) + ":" + coord(lng);
    }

    private String coord(BigDecimal value) {
        return value == null ? "null" : value.setScale(COORD_SCALE, RoundingMode.HALF_UP).toPlainString();
    }
}
