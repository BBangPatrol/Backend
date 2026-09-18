package com.bbangpatrol.ocr.service;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.dto.ReceiptTokenPayload;
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
    // 승인번호는 숫자, 사업자번호는 숫자와 하이픈, 구는 enum 이름이라 이 구분자와 겹치지 않는다
    private static final String DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;

    @Value("${receipt.verification-secret}")
    private String secret;

    public String issueToken(Long userId, ReceiptTokenPayload payload) {
        String nonce = UUID.randomUUID().toString();

        String raw = userId + ":" + payload.receiptNum() + ":" + nonce;
        String token = createHmac(raw);

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                serialize(payload),
                TTL
        );

        return token;
    }

    public ReceiptTokenPayload consumeToken(String token) {
        String key = KEY_PREFIX + token;

        String stored = redisTemplate.opsForValue()
                .getAndDelete(key);

        if (stored == null) {
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }

        return deserialize(stored);
    }

    private String serialize(ReceiptTokenPayload payload) {
        return String.join(
                DELIMITER,
                payload.receiptNum(),
                payload.businessNumber(),
                payload.region() == null ? Region.NONE.name() : payload.region().name()
        );
    }

    private ReceiptTokenPayload deserialize(String stored) {
        // 배포 직전에 발급된 예전 형식 토큰이 남아 있을 수 있다. 재인증을 유도한다
        String[] parts = stored.split("\\" + DELIMITER, -1);
        if (parts.length != 3) {
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }

        return new ReceiptTokenPayload(parts[0], parts[1], Region.valueOf(parts[2]));
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
