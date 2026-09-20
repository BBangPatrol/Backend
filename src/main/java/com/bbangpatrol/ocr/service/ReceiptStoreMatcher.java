package com.bbangpatrol.ocr.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영수증이 "사용자가 고른 그 가게"의 것인지 상호와 주소로 본다.
 *
 * 사업자번호를 아는 가게는 번호로 거르는 것이 가장 정확하지만, 번호만으로는 지점을 구분하지 못한다
 * (성심당처럼 여러 지점이 한 법인 번호를 쓰면 대전역점 영수증으로 본점 인증이 통과된다).
 * 그래서 주소는 번호를 아는 가게에도 함께 본다. 상호 대조는 번호를 모르는 가게에서만 쓴다.
 */
@Component
public class ReceiptStoreMatcher {

    /** 주소 대조 결과. UNKNOWN 은 "다르다"가 아니라 "비교할 수 없었다"는 뜻이다. */
    public enum AddressMatch {
        MATCH, MISMATCH, UNKNOWN
    }

    // 주소를 정규화할 때 공백·쉼표 자리에 넣는 경계 문자.
    // 전부 지워버리면 "80-7, 1층" 이 "80-71층" 이 돼 건물번호가 80-71 로 읽힌다.
    private static final String BOUNDARY = "#";

    // "둔산로155", "대종로480번길15" 에서 도로명과 첫 번째 번호를 뽑는다.
    // 공백을 지운 뒤에 쓰므로 "둔산남로 175번길 10" 과 "둔산남로175번길 10" 이 같은 값이 된다.
    // 도로명에도 숫자가 들어간다 ("창조2길", "테크노4로"). 이름 부분에 숫자를 허용하되
    // 수량자가 게을러서 "대종로480번길15" 는 "대종로"+"480" 으로 먼저 끊긴다.
    // 도로명과 번호 사이의 경계(#)는 있어도 되고 없어도 된다 — "대종로 480번길" 과 "대종로480번길" 이 같아야 한다.
    // 이름 안쪽에도 경계를 허용한다. OCR 이 "유성 대로" 처럼 도로명을 띄어 읽는 일이 잦은데,
    // 이걸 막으면 "유성#대로" 를 한 도로명으로 못 읽고 "대로" 만 잡아서 엉뚱한 값이 나온다.
    // 앞쪽 행정구역까지 같이 걸리지만 roadName 이 떼어낸다.
    private static final Pattern ROAD = Pattern.compile(
            "([가-힣A-Za-z0-9][가-힣A-Za-z0-9" + BOUNDARY + "]{0,29}?(?:대로|로|길))"
                    + BOUNDARY + "?(\\d+(?:-\\d+)?)");

    // 도로명을 못 읽었을 때 쓰는 거친 비교. 이 서비스는 대전 전용이라 자치구만 본다.
    private static final Pattern DISTRICT = Pattern.compile("(동구|중구|서구|유성구|대덕구)");

    // 시·도가 서로 다르면 그 아래는 볼 필요가 없다 (서울 중구 영수증으로 대전 중구 가게를 인증하는 경우)
    private static final Pattern CITY = Pattern.compile(
            "(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)");

    // 한두 글자는 아무 데나 걸린다. "빵" 같은 값으로 통과하지 않게 최소 길이를 둔다.
    private static final int MIN_NAME_LENGTH = 2;

    /**
     * 상호 대조. 표기 차이(공백·쉼표)와 지점명이 붙는 경우를 감안해 한쪽이 다른 쪽을 품으면 같다고 본다.
     * "빵, 한모금" ↔ "빵한모금", "성심당" ↔ "성심당 본점" 이 모두 통과해야 한다.
     */
    public boolean matchesName(String bakeryName, String receiptName) {
        String stored = normalizeName(bakeryName);
        String parsed = normalizeName(receiptName);

        if (stored.length() < MIN_NAME_LENGTH || parsed.length() < MIN_NAME_LENGTH) {
            return false;
        }
        return stored.contains(parsed) || parsed.contains(stored);
    }

    /**
     * 주소 대조. 도로명+번호가 양쪽에 다 있으면 그것으로 판정하고,
     * 지번 주소만 찍힌 영수증처럼 도로명을 못 뽑으면 자치구까지만 본다.
     * 어느 쪽도 볼 수 없으면 UNKNOWN 이다 — 호출측이 경로에 따라 다르게 다룬다.
     */
    public AddressMatch matchAddress(String bakeryAddress, String receiptAddress) {
        String stored = normalizeAddress(bakeryAddress);
        String parsed = normalizeAddress(receiptAddress);

        if (stored.isEmpty() || parsed.isEmpty()) {
            return AddressMatch.UNKNOWN;
        }

        String storedCity = find(CITY, stored);
        String parsedCity = find(CITY, parsed);
        if (storedCity != null && parsedCity != null && !storedCity.equals(parsedCity)) {
            return AddressMatch.MISMATCH;
        }

        String storedRoad = findRoad(stored);
        String parsedRoad = findRoad(parsed);
        if (storedRoad != null && parsedRoad != null) {
            return storedRoad.equals(parsedRoad) ? AddressMatch.MATCH : AddressMatch.MISMATCH;
        }

        String storedDistrict = find(DISTRICT, stored);
        String parsedDistrict = find(DISTRICT, parsed);
        if (storedDistrict != null && parsedDistrict != null) {
            return storedDistrict.equals(parsedDistrict) ? AddressMatch.MATCH : AddressMatch.MISMATCH;
        }

        return AddressMatch.UNKNOWN;
    }

    // 공백과 구두점을 버리고 소문자로 맞춘다
    private String normalizeName(String value) {
        if (value == null) return "";
        return value.toLowerCase().replaceAll("[^0-9a-z가-힣]", "");
    }

    // 공백과 구두점은 경계 문자로 바꾼다. 번지의 하이픈(145-1)은 그대로 둔다
    private String normalizeAddress(String value) {
        if (value == null) return "";
        return value.toLowerCase().replaceAll("[^0-9a-z가-힣-]+", BOUNDARY);
    }

    // 도로명 + 첫 번째 번호를 하나의 문자열로 (예: "둔산로155")
    private String findRoad(String address) {
        Matcher matcher = ROAD.matcher(address);
        return matcher.find() ? roadName(matcher.group(1)) + matcher.group(2) : null;
    }

    /**
     * 공백을 지운 뒤라 도로명 앞의 행정구역이 그대로 붙어 나온다
     * ("대전광역시서구둔산로"). 마지막 시·군·구 뒤만 남겨 도로명을 떼어낸다.
     * 한쪽 주소에만 "대전광역시"가 붙어 있어도 같은 값이 나와야 하기 때문이다.
     */
    private String roadName(String captured) {
        for (int i = captured.length() - 2; i >= 0; i--) {
            char boundary = captured.charAt(i);
            if (boundary == '시' || boundary == '군' || boundary == '구') {
                return stripBoundary(captured.substring(i + 1));
            }
        }
        return stripBoundary(captured);
    }

    // 도로명 안에 남은 경계는 버린다. "유성#대로" 와 "유성대로" 가 같은 값이어야 한다
    private String stripBoundary(String value) {
        return value.replace(BOUNDARY, "");
    }

    private String find(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
