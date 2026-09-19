package com.bbangpatrol.ocr.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.service.ReceiptTokenService.ReceiptTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이 토큰이 1단계(영수증 인증)와 2단계(방문 등록) 사이의 유일한 연결이다.
 * 여기 담기지 않은 값은 2단계가 요청 본문에서 받게 되고, 그러면 사용자가 정하는 값이 된다.
 */
@ExtendWith(MockitoExtension.class)
class ReceiptTokenServiceTest {

    private static final long USER_ID = 6L;
    private static final long STORE_ID = 101L;
    private static final LocalDate DATE = LocalDate.of(2026, 9, 5);

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReceiptTokenService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptTokenService(redisTemplate);
        ReflectionTestUtils.setField(service, "secret", "test-secret");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("발급한 내용이 그대로 돌아온다")
    void roundTripsTicket() {
        String token = service.issueToken(
                new ReceiptTicket(USER_ID, STORE_ID, "68719332", "314-22-67770", 11300, DATE));

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("receipt:verification:" + token), value.capture(), any(Duration.class));
        when(valueOperations.getAndDelete(anyString())).thenReturn(value.getValue());

        ReceiptTicket ticket = service.consumeToken(token, USER_ID, STORE_ID);

        assertThat(ticket.userId()).isEqualTo(USER_ID);
        assertThat(ticket.storeId()).isEqualTo(STORE_ID);
        assertThat(ticket.receiptNum()).isEqualTo("68719332");
        assertThat(ticket.businessNumber()).isEqualTo("314-22-67770");
        assertThat(ticket.amount()).isEqualTo(11300);
        assertThat(ticket.date()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("다른 가게에서 쓰면 막는다")
    void rejectsOtherStore() {
        when(valueOperations.getAndDelete(anyString()))
                .thenReturn("v2|6|101|314-22-67770|68719332|11300|2026-09-05");

        // A 가게에서 받은 토큰으로 B 가게 방문을 인정받으면 1단계의 가게 검증이 통째로 무의미해진다
        assertThatThrownBy(() -> service.consumeToken("token", USER_ID, 999L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RECEIPT_TOKEN);
    }

    @Test
    @DisplayName("다른 사용자가 쓰면 막는다")
    void rejectsOtherUser() {
        when(valueOperations.getAndDelete(anyString()))
                .thenReturn("v2|6|101|314-22-67770|68719332|11300|2026-09-05");

        assertThatThrownBy(() -> service.consumeToken("token", 7L, STORE_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RECEIPT_TOKEN);
    }

    @Test
    @DisplayName("없는 토큰이면 막는다")
    void rejectsMissingToken() {
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.consumeToken("token", USER_ID, STORE_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RECEIPT_TOKEN);
    }

    @Test
    @DisplayName("배포 직전에 발급된 옛 토큰(승인번호만)도 받아준다")
    void acceptsLegacyToken() {
        // 옛 토큰은 승인번호 한 줄뿐이다. 나머지가 null 이면 2단계가 예전처럼 본문 값으로 돌아간다.
        when(valueOperations.getAndDelete(anyString())).thenReturn("68719332");

        ReceiptTicket ticket = service.consumeToken("token", USER_ID, STORE_ID);

        assertThat(ticket.receiptNum()).isEqualTo("68719332");
        assertThat(ticket.userId()).isNull();
        assertThat(ticket.storeId()).isNull();
        assertThat(ticket.amount()).isNull();
        assertThat(ticket.date()).isNull();
    }
}
