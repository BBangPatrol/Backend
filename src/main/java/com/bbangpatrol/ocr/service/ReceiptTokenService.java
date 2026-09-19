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

    /**
     * 인증을 통과한 영수증의 정보. 방문 등록(2단계)이 중복 방지 해시를 만들 때 쓴다.
     * businessNumber 는 영수증에서 읽은 값이고, 옛 토큰에는 없어서 null 일 수 있다.
     */
    public record ReceiptTicket(String receiptNum, String businessNumber) {
    }

    // Redis 값은 "사업자번호|승인번호". 승인번호에는 구분자가 들어가지 않는다
    private static final String DELIMITER = "|";

    public String issueToken(Long userId, String receiptNum, String businessNumber) {
        String nonce = UUID.randomUUID().toString();

        String raw = userId + ":" + receiptNum + ":" + nonce;
        String token = createHmac(raw);

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                (businessNumber == null ? "" : businessNumber) + DELIMITER + receiptNum,
                TTL
        );

        return token;
    }

    public ReceiptTicket consumeToken(String token) {
        String key = KEY_PREFIX + token;

        String stored = redisTemplate.opsForValue()
                .getAndDelete(key);

        if (stored == null) {
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }

        // 배포 직전에 발급돼 아직 살아 있는 옛 토큰은 승인번호만 들어 있다 (TTL 10분)
        int boundary = stored.indexOf(DELIMITER);
        if (boundary < 0) {
            return new ReceiptTicket(stored, null);
        }

        String businessNumber = stored.substring(0, boundary);
        return new ReceiptTicket(
                stored.substring(boundary + 1),
                businessNumber.isBlank() ? null : businessNumber
        );
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