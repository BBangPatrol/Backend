package com.bbangpatrol.ocr.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 1단계(영수증 인증)가 확인한 내용을 2단계(방문 등록)까지 옮기는 통로다.
 *
 * 예전에는 승인번호만 담아서, 2단계가 저장하는 금액·날짜와 가게가 전부 요청 본문에서 왔다.
 * 그러면 1단계에서 무엇을 검증하든 2단계가 다른 값으로 저장할 수 있어 검증이 의미를 잃는다
 * (금액을 1원씩 바꾸면 중복 방지 해시가 매번 달라져 같은 영수증을 계속 쓸 수 있었다).
 * 그래서 영수증에서 읽은 값을 통째로 여기 담고, 2단계는 본문 대신 이 값을 쓴다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptTokenService {

    private static final String KEY_PREFIX = "receipt:verification:";
    private static final Duration TTL = Duration.ofMinutes(10);

    // Redis 값 형식: v2|userId|storeId|사업자번호|승인번호|금액|날짜
    // 맨 앞 표식으로 옛 토큰과 구분한다. 어느 항목에도 구분자가 들어가지 않는다.
    private static final String VERSION = "v2";
    private static final String DELIMITER = "|";
    private static final int FIELD_COUNT = 7;

    private final StringRedisTemplate redisTemplate;

    @Value("${receipt.verification-secret}")
    private String secret;

    /**
     * 인증을 통과한 영수증의 내용. 2단계가 저장·지급에 쓰는 값이 모두 여기 들어 있다.
     * 배포 직전에 발급된 옛 토큰에는 승인번호밖에 없어 나머지가 null 일 수 있다.
     */
    public record ReceiptTicket(
            Long userId,
            Long storeId,
            String receiptNum,
            String businessNumber,
            Integer amount,
            LocalDate date
    ) {
    }

    public String issueToken(ReceiptTicket ticket) {
        String nonce = UUID.randomUUID().toString();

        String raw = ticket.userId() + ":" + ticket.receiptNum() + ":" + nonce;
        String token = createHmac(raw);

        redisTemplate.opsForValue().set(KEY_PREFIX + token, encode(ticket), TTL);

        return token;
    }

    /**
     * 토큰을 한 번만 쓰도록 소모하고 내용을 돌려준다.
     * 발급받은 사용자·가게가 아니면 거절한다 — 그러지 않으면 A 가게에서 받은 토큰으로
     * B 가게 방문을 인정받을 수 있다(1단계의 가게 검증이 통째로 우회된다).
     *
     * 검증보다 삭제가 먼저인 이유: 읽고 나서 지우면 그 틈에 같은 토큰이 두 번 통과할 수 있다.
     * 토큰은 발급받은 본인에게만 전달되므로 남의 토큰을 태워 없애는 상황은 전제하지 않는다.
     */
    public ReceiptTicket consumeToken(String token, Long userId, Long storeId) {
        String key = KEY_PREFIX + token;

        String stored = redisTemplate.opsForValue().getAndDelete(key);

        if (stored == null) {
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }

        ReceiptTicket ticket = decode(stored);

        if (ticket.userId() != null && !ticket.userId().equals(userId)) {
            log.warn("[RECEIPT] 다른 사용자에게 발급된 토큰이다. 발급={}, 요청={}", ticket.userId(), userId);
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }
        if (ticket.storeId() != null && !ticket.storeId().equals(storeId)) {
            log.warn("[RECEIPT] 다른 가게에서 발급된 토큰이다. 발급={}, 요청={}", ticket.storeId(), storeId);
            throw new ApiException(ErrorCode.INVALID_RECEIPT_TOKEN);
        }
        return ticket;
    }

    private String encode(ReceiptTicket ticket) {
        return String.join(DELIMITER,
                VERSION,
                text(ticket.userId()),
                text(ticket.storeId()),
                text(ticket.businessNumber()),
                text(ticket.receiptNum()),
                text(ticket.amount()),
                text(ticket.date()));
    }

    /**
     * 배포 직전에 발급돼 아직 살아 있는 옛 토큰은 승인번호만 들어 있다 (TTL 10분).
     * 그 토큰은 예전처럼 요청 본문 값으로 처리되도록 나머지를 null 로 둔다.
     */
    private ReceiptTicket decode(String stored) {
        String[] parts = stored.split("\\" + DELIMITER, -1);

        if (parts.length != FIELD_COUNT || !VERSION.equals(parts[0])) {
            return new ReceiptTicket(null, null, stored, null, null, null);
        }

        return new ReceiptTicket(
                longOrNull(parts[1]),
                longOrNull(parts[2]),
                blankToNull(parts[4]),
                blankToNull(parts[3]),
                intOrNull(parts[5]),
                dateOrNull(parts[6])
        );
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Long longOrNull(String value) {
        try {
            return value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intOrNull(String value) {
        try {
            return value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate dateOrNull(String value) {
        try {
            return value.isBlank() ? null : LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
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
