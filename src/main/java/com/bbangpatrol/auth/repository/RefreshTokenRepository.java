package com.bbangpatrol.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    @Value("${REFRESH_TOKEN_EXPIRED_TIME}")
    private int expiredTime;

    private String PREFIX = "RT:"; // Refresh Token의 약자

    public void save(Long userId, String refreshToken) { // Refresh Token을 저장하기 위한 메서드
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                refreshToken,
                expiredTime,
                TimeUnit.DAYS
        ); // 유효 기간을 7일로 설정해서 저장
    }

    public boolean isValid(Long userId, String refreshToken) { // 사용자의 refresh token이 redis에 있는지 확인
        String stored = redisTemplate.opsForValue().get(PREFIX + userId);
        // 값이 null이 아니면서, 저장된 토큰과 전달받은 토큰이 일치하면 유효
        return stored != null && stored.equals(refreshToken);
    }

    public void delete(Long userId) { // 토큰 삭제를 위한 메서드
        redisTemplate.delete(PREFIX + userId);
    }
}
