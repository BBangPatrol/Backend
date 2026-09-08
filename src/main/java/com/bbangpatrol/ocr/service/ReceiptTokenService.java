package com.bbangpatrol.ocr.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptTokenService {

    private static final String KEY_PREFIX = "receipt:verification:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Value("${receipt.verification-secret}")
    private String secret;

    public String issueToken(Long userId, String receiptNum) {
        String nonce = UUID.randomUUID().toString();

        String raw = userId + ":" + receiptNum + ":" + nonce;
        String token = createHmac(raw);

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                receiptNum,
                TTL
        );

        return token;
    }

    public String consumeToken(String token) {
        String key = KEY_PREFIX + token;

        String receiptNum = redisTemplate.opsForValue()
                .getAndDelete(key);

        if (receiptNum == null) {
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }

        return receiptNum;
    }

    private String createHmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(keySpec);

            byte[] hash = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException("영수증 인증 토큰 생성에 실패했습니다.", e);
        }
    }
}