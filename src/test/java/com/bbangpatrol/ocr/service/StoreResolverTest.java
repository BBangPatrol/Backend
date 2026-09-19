package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.bbangpatrol.ocr.service.StoreResolver.Resolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 가게를 고르지 않는 흐름에서는 영수증이 유일한 근거다. 판별을 틀리면 남의 가게에 방문이 쌓인다.
 * 실제로 받아 본 영수증 5장(슬로우브레드·하레하레 둔산점·몽심 중교로점·콜드버터 시청점·마트)을 기준으로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class StoreResolverTest {

    @Mock
    private BakeryRepository bakeryRepository;

    private StoreResolver storeResolver;

    private final Bakery slowBread = bakery(75L, "슬로우브레드",
            "대전광역시 유성구 유성대로 1734-1, 1층", "314-22-67770");
    private final Bakery harehare = bakery(106L, "하레하레",
            "대전광역시 서구 둔산로 155 크로바아파트 제상가동 1층", "368-87-03826");
    private final Bakery mongsim = bakery(1L, "몽심",
            "대전광역시 대덕구 한남로 38번길 28, 1층", "674-58-00286");
    private final Bakery coldButter = bakery(27L, "콜드버터베이크샵",
            "대전광역시 중구 중앙로 112번길 37, 1층", "793-59-00304");
    // 사업자번호를 모르는 가게 (가이드북에만 실린 18곳 중 하나)
    private final Bakery yeoniga = bakery(105L, "연이가 베이크샵",
            "대전광역시 서구 둔산남로 175번길 10, 102호", null);

    @BeforeEach
    void setUp() {
        storeResolver = new StoreResolver(bakeryRepository, new ReceiptStoreMatcher());
        when(bakeryRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(mongsim, coldButter, slowBread, yeoniga, harehare));
    }

    @Test
    @DisplayName("사업자번호가 맞는 가게를 찾아낸다 (슬로우브레드 영수증)")
    void resolvesByBusinessNumber() {
        Resolution resolution = storeResolver.resolve(
                receipt("슬로우브레드", "대전 유성구 유성대로 1734-1 (전민동)", "314-22-67770"));

        assertThat(resolution.isMatched()).isTrue();
        assertThat(resolution.matched().getId()).isEqualTo(75L);
    }

    @Test
    @DisplayName("상호가 영문으로 찍혀도 사업자번호로 찾는다 (하레하레 둔산점 영수증)")
    void resolvesByNumberEvenWhenNameDiffers() {
        Resolution resolution = storeResolver.resolve(
                receipt("하레하레둔산점", "대전광역시 서구 둔산로 155 제상가동 1,2층(둔산동, 크로바아파트)", "368-87-03826"));

        assertThat(resolution.matched().getId()).isEqualTo(106L);
    }

    @Test
    @DisplayName("사업자번호를 모르는 가게는 상호와 주소로 찾는다 (연이가)")
    void resolvesByNameAndAddress() {
        Resolution resolution = storeResolver.resolve(
                receipt("연이가", "대전 서구 둔산남로175번길 10", "123-45-67890"));

        assertThat(resolution.matched().getId()).isEqualTo(105L);
    }

    @Test
    @DisplayName("등록되지 않은 지점의 영수증은 찾지 않는다 (몽심 중교로점 영수증)")
    void doesNotResolveUnregisteredBranch() {
        // DB 에 있는 몽심은 한남대점이고, 이 영수증은 중교로점이다
        Resolution resolution = storeResolver.resolve(
                receipt("CREATIVE MONGSIM", "대전 중구 중교로 29 1층", "816-86-02784"));

        assertThat(resolution.isMatched()).isFalse();
        assertThat(resolution.isAmbiguous()).isFalse();
    }

    @Test
    @DisplayName("같은 브랜드라도 등록된 지점과 주소가 다르면 인정하지 않는다")
    void doesNotResolveWhenAddressDiffersFromRegisteredBranch() {
        // 사업자번호는 등록된 가게와 같지만 다른 지점 주소인 경우 (법인번호를 여러 지점이 공유)
        Resolution resolution = storeResolver.resolve(
                receipt("하레하레 갤러리아점", "대전 서구 대덕대로 211", "368-87-03826"));

        assertThat(resolution.isMatched()).isFalse();
    }

    @Test
    @DisplayName("빵집이 아닌 영수증은 찾지 않는다 (마트 영수증)")
    void doesNotResolveUnknownStore() {
        Resolution resolution = storeResolver.resolve(
                receipt("인동농협하나로마트", "경북 구미시 인동남길 106", "513-82-00249"));

        assertThat(resolution.isMatched()).isFalse();
        assertThat(resolution.candidates()).isEmpty();
    }

    @Test
    @DisplayName("주소를 못 읽으면 번호를 모르는 가게는 특정하지 않는다")
    void doesNotResolveWithoutAddress() {
        Resolution resolution = storeResolver.resolve(receipt("연이가", null, "123-45-67890"));

        assertThat(resolution.isMatched()).isFalse();
    }

    private ReceiptParseResult receipt(String name, String address, String businessNumber) {
        return new ReceiptParseResult(name, address, "2026-09-19", 11300, "소금빵", "68719332", businessNumber);
    }

    private Bakery bakery(Long id, String name, String address, String businessNumber) {
        return Bakery.builder().id(id).name(name).address(address).businessNumber(businessNumber).build();
    }
}
