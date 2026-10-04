package com.bbangpatrol.visit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 영수증을 두 번 인정하지 않는 장치의 뿌리. 여기서 만든 해시가 visit_detail.receipt_hash 에 저장되고
 * VerificationServiceImpl 이 existsByReceiptHash 로 중복을 막는다.
 * 같은 영수증이 매번 같은 값을 내야 하고(그래야 재사용이 걸린다),
 * 항목이 하나라도 다르면 다른 값이 나와야 한다(그래야 정상 방문이 막히지 않는다).
 */
class ReceiptHashServiceTest {

    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String RECEIPT_NUM = "0001";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 5);
    private static final int AMOUNT = 12000;

    private final ReceiptHashService service = new ReceiptHashService();

    @Test
    @DisplayName("같은 영수증은 항상 같은 해시를 낸다")
    void isDeterministic() {
        String first = service.create(BUSINESS_NUMBER, RECEIPT_NUM, DATE, AMOUNT);
        String second = service.create(BUSINESS_NUMBER, RECEIPT_NUM, DATE, AMOUNT);

        assertThat(first).isEqualTo(second);
        // SHA-256 을 hex 로 편 값
        assertThat(first).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("사업자번호는 하이픈이 있든 없든 같은 해시가 된다")
    void normalizesBusinessNumber() {
        String hyphenated = service.create("123-45-67890", RECEIPT_NUM, DATE, AMOUNT);
        String plain = service.create("1234567890", RECEIPT_NUM, DATE, AMOUNT);
        String spaced = service.create(" 123 45 67890 ", RECEIPT_NUM, DATE, AMOUNT);

        // 표기만 다른 같은 가게가 서로 다른 해시를 받으면 같은 영수증을 두 번 쓸 수 있게 된다
        assertThat(plain).isEqualTo(hyphenated);
        assertThat(spaced).isEqualTo(hyphenated);
    }

    @Test
    @DisplayName("사업자번호가 없어도 예외 없이 해시를 만든다")
    void toleratesNullBusinessNumber() {
        assertThat(service.create(null, RECEIPT_NUM, DATE, AMOUNT))
                .isNotNull()
                .isEqualTo(service.create("", RECEIPT_NUM, DATE, AMOUNT));
    }

    @Test
    @DisplayName("승인번호, 날짜, 금액 중 하나만 달라도 다른 해시가 된다")
    void differsWhenAnyFieldChanges() {
        String base = service.create(BUSINESS_NUMBER, RECEIPT_NUM, DATE, AMOUNT);

        assertThat(service.create(BUSINESS_NUMBER, "0002", DATE, AMOUNT)).isNotEqualTo(base);
        assertThat(service.create(BUSINESS_NUMBER, RECEIPT_NUM, DATE.plusDays(1), AMOUNT)).isNotEqualTo(base);
        assertThat(service.create(BUSINESS_NUMBER, RECEIPT_NUM, DATE, AMOUNT + 1)).isNotEqualTo(base);
        assertThat(service.create("999-99-99999", RECEIPT_NUM, DATE, AMOUNT)).isNotEqualTo(base);
    }

    @Test
    @DisplayName("구분자 위치가 밀려도 다른 영수증으로 본다")
    void isNotVulnerableToFieldShifting() {
        // "1234:5:..." 와 "123:45:..." 가 같은 문자열로 합쳐지면 서로 다른 영수증이 한 해시를 갖게 된다
        String a = service.create("1234", "5", DATE, AMOUNT);
        String b = service.create("123", "45", DATE, AMOUNT);

        assertThat(a).isNotEqualTo(b);
    }
}
