package com.bbangpatrol.ocr.service;

import com.bbangpatrol.ocr.service.ReceiptStoreMatcher.AddressMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사업자번호를 모르는 가게(가이드북에만 실린 곳)의 인증이 이 대조에 걸려 있다.
 * 느슨하면 아무 영수증이나 통과하고, 빡빡하면 정상 방문이 막힌다.
 * 특히 주소는 "다르다"와 "못 읽었다"를 구분해야 한다 — 호출측이 경로에 따라 다르게 다룬다.
 */
class ReceiptStoreMatcherTest {

    private final ReceiptStoreMatcher matcher = new ReceiptStoreMatcher();

    @Test
    @DisplayName("표기가 달라도 같은 상호로 본다")
    void matchesNameIgnoringNotation() {
        // 가이드북은 "빵, 한모금", 영수증은 "빵한모금" 으로 찍힌다
        assertThat(matcher.matchesName("빵, 한모금", "빵한모금")).isTrue();
        assertThat(matcher.matchesName("연이가 베이크샵", "연이가")).isTrue();
        assertThat(matcher.matchesName("다소리과자점", "다소리 과자점")).isTrue();
    }

    @Test
    @DisplayName("지점명이 붙어 있어도 같은 상호로 본다")
    void matchesNameWithBranchSuffix() {
        assertThat(matcher.matchesName("성심당", "성심당 본점")).isTrue();
        assertThat(matcher.matchesName("하레하레", "하레하레 둔산점")).isTrue();
    }

    @Test
    @DisplayName("다른 가게 이름은 통과하지 않는다")
    void rejectsOtherName() {
        assertThat(matcher.matchesName("다소리과자점", "몽심")).isFalse();
        assertThat(matcher.matchesName("성심당", null)).isFalse();
        // 한 글자는 어디에나 걸리므로 통과시키지 않는다
        assertThat(matcher.matchesName("빵, 한모금", "빵")).isFalse();
    }

    @Test
    @DisplayName("도로명과 번호가 같으면 같은 매장으로 본다")
    void matchesAddressByRoad() {
        assertThat(matcher.matchAddress(
                "대전광역시 서구 둔산로 155 크로바아파트 제상가동 1층",
                "대전 서구 둔산로 155"))
                .isEqualTo(AddressMatch.MATCH);

        // 띄어쓰기만 다른 같은 주소 (가이드북은 "대종로 480번길", 영수증은 "대종로480번길")
        assertThat(matcher.matchAddress(
                "대전광역시 중구 대종로 480번길 15",
                "대전광역시 중구 대종로480번길 15"))
                .isEqualTo(AddressMatch.MATCH);
    }

    @Test
    @DisplayName("실제 영수증(슬로우브레드)의 값으로 통과한다")
    void matchesRealReceipt() {
        // 2026-08-15 슬로우브레드 영수증을 실제 프롬프트로 파싱한 결과:
        //   bakeryName="슬로우브레드", address="대전 유성구 유성대로 1734-1"
        // 시드(V2)의 주소는 "대전광역시 유성구 유성대로 1734-1, 1층" 이다.
        String seededName = "슬로우브레드";
        String seededAddress = "대전광역시 유성구 유성대로 1734-1, 1층";

        assertThat(matcher.matchesName(seededName, "슬로우브레드")).isTrue();
        assertThat(matcher.matchAddress(seededAddress, "대전 유성구 유성대로 1734-1"))
                .isEqualTo(AddressMatch.MATCH);
    }

    @Test
    @DisplayName("숫자가 들어간 도로명도 뽑아낸다")
    void matchesNumberedRoad() {
        // 시드에 "창조2길", "테크노4로" 같은 주소가 있다. 숫자를 못 읽으면 구 단위 비교로 떨어진다
        assertThat(matcher.matchAddress("대전광역시 동구 창조2길 11", "대전 동구 창조2길 11"))
                .isEqualTo(AddressMatch.MATCH);
        assertThat(matcher.matchAddress("대전광역시 유성구 테크노4로 80-7, 1층 101호", "대전 유성구 테크노4로 80-7"))
                .isEqualTo(AddressMatch.MATCH);
        assertThat(matcher.matchAddress("대전광역시 동구 창조2길 11", "대전 동구 창조1길 11"))
                .isEqualTo(AddressMatch.MISMATCH);
    }

    @Test
    @DisplayName("같은 브랜드라도 다른 지점 주소면 막는다")
    void rejectsOtherBranch() {
        // 하레하레 둔산점 ↔ 갤러리아 타임월드점
        assertThat(matcher.matchAddress(
                "대전광역시 서구 둔산로 155 크로바아파트 제상가동 1층",
                "대전 서구 대덕대로 211"))
                .isEqualTo(AddressMatch.MISMATCH);

        // 성심당 본점 ↔ 대전역점. 사업자번호가 같아도 여기서 걸린다
        assertThat(matcher.matchAddress(
                "대전광역시 중구 대종로 480번길 15",
                "대전 동구 중앙로 215"))
                .isEqualTo(AddressMatch.MISMATCH);
    }

    @Test
    @DisplayName("지번 주소로 찍힌 영수증은 자치구까지만 본다")
    void fallsBackToDistrict() {
        // 도로명을 뽑을 수 없으면 구라도 맞아야 한다
        assertThat(matcher.matchAddress(
                "대전광역시 중구 대종로 480번길 15",
                "대전 중구 은행동 145-1"))
                .isEqualTo(AddressMatch.MATCH);

        assertThat(matcher.matchAddress(
                "대전광역시 중구 대종로 480번길 15",
                "대전 서구 둔산동 1509"))
                .isEqualTo(AddressMatch.MISMATCH);
    }

    @Test
    @DisplayName("시·도가 다르면 그 아래는 보지 않는다")
    void rejectsOtherCity() {
        // 같은 '중구' 라도 서울과 대전은 다른 곳이다
        assertThat(matcher.matchAddress(
                "대전광역시 중구 대종로 480번길 15",
                "서울특별시 중구 세종대로 110"))
                .isEqualTo(AddressMatch.MISMATCH);
    }

    @Test
    @DisplayName("비교할 주소가 없으면 UNKNOWN 이다")
    void unknownWhenNothingToCompare() {
        assertThat(matcher.matchAddress("대전광역시 중구 대종로 480번길 15", null))
                .isEqualTo(AddressMatch.UNKNOWN);
        assertThat(matcher.matchAddress(null, "대전 중구 대종로480번길 15"))
                .isEqualTo(AddressMatch.UNKNOWN);
        // 양쪽 다 도로명도 구도 못 뽑는 경우
        assertThat(matcher.matchAddress("대전 은행동", "은행동 일원"))
                .isEqualTo(AddressMatch.UNKNOWN);
    }
}
