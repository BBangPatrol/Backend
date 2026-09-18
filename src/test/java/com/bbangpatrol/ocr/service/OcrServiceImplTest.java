package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.BakeryBusinessNumber;
import com.bbangpatrol.bakery.repository.BakeryBusinessNumberRepository;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.bbangpatrol.ocr.dto.ReceiptTokenPayload;
import com.bbangpatrol.visit.service.ReceiptDuplicateChecker;
import com.bbangpatrol.visit.service.ReceiptHashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 빵산책 지도는 브랜드당 한 매장만 싣는다. 같은 브랜드의 다른 지점은 사업자번호가 달라
 * 여기서 걸러지는데, 너무 조이면 실제 방문이 막히고 너무 풀면 아무 영수증이나 통과한다.
 * 그 경계를 지키는 것이 이 테스트다.
 */
@ExtendWith(MockitoExtension.class)
class OcrServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long STORE_ID = 101L;
    private static final String MAIN_NUMBER = "674-58-00286";
    private static final String BRANCH_NUMBER = "816-86-02784";
    private static final String RECEIPT_NUM = "68076898";
    private static final String HASH = "hashed-receipt";
    private static final String TOKEN = "issued-token";

    @Mock
    private ReceiptImageValidator imageValidator;
    @Mock
    private OcrClient ocrClient;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private BakeryBusinessNumberRepository bakeryBusinessNumberRepository;
    @Mock
    private ReceiptTokenService receiptTokenService;
    @Mock
    private ReceiptHashService receiptHashService;
    @Mock
    private ReceiptDuplicateChecker receiptDuplicateChecker;

    @InjectMocks
    private OcrServiceImpl ocrService;

    @Test
    @DisplayName("지도에 실린 대표 매장 영수증이면 그 매장의 구로 집계한다")
    void acceptsMainStoreReceipt() {
        givenParsed(MAIN_NUMBER, "몽심");
        givenBakery(bakery(MAIN_NUMBER), List.of());
        givenHashAndToken();

        ocrService.getInfo(USER_ID, STORE_ID, receiptFile());

        assertThat(capturedPayload().region()).isEqualTo(Region.DAEDEOK);
    }

    @Test
    @DisplayName("등록해 둔 지점 영수증이면 통과하고 지점이 있는 구로 집계한다")
    void acceptsRegisteredBranchReceipt() {
        givenParsed(BRANCH_NUMBER, "(주)리황");
        givenBakery(bakery(MAIN_NUMBER), List.of(branch(BRANCH_NUMBER, Region.JUNG)));
        givenHashAndToken();

        ocrService.getInfo(USER_ID, STORE_ID, receiptFile());

        ReceiptTokenPayload payload = capturedPayload();
        // 대덕구로 가면 중구에서 산 빵으로 대덕구 미션을 깰 수 있게 된다
        assertThat(payload.region()).isEqualTo(Region.JUNG);
        assertThat(payload.businessNumber()).isEqualTo(BRANCH_NUMBER);
    }

    @Test
    @DisplayName("지점에 구가 없으면 지도에 실린 매장의 구를 따른다")
    void fallsBackToBakeryRegionWhenBranchHasNone() {
        givenParsed(BRANCH_NUMBER, "(주)리황");
        givenBakery(bakery(MAIN_NUMBER), List.of(branch(BRANCH_NUMBER, null)));
        givenHashAndToken();

        ocrService.getInfo(USER_ID, STORE_ID, receiptFile());

        assertThat(capturedPayload().region()).isEqualTo(Region.DAEDEOK);
    }

    @Test
    @DisplayName("해시는 DB 대표번호가 아니라 영수증에 찍힌 번호로 만든다")
    void hashesWithReceiptBusinessNumber() {
        givenParsed(BRANCH_NUMBER, "(주)리황");
        givenBakery(bakery(MAIN_NUMBER), List.of(branch(BRANCH_NUMBER, Region.JUNG)));
        givenHashAndToken();

        ocrService.getInfo(USER_ID, STORE_ID, receiptFile());

        // 대표번호로 해시를 만들면 본점과 지점 영수증이 서로를 막아버린다
        verify(receiptHashService).create(BRANCH_NUMBER, RECEIPT_NUM, LocalDate.of(2026, 9, 15), 15800);
    }

    @Test
    @DisplayName("하이픈이나 공백이 달라도 같은 번호로 본다")
    void ignoresFormattingDifferences() {
        givenParsed(" 67458 00286 ", "몽심");
        givenBakery(bakery(MAIN_NUMBER), List.of());
        givenHashAndToken();

        ocrService.getInfo(USER_ID, STORE_ID, receiptFile());

        assertThat(capturedPayload().region()).isEqualTo(Region.DAEDEOK);
    }

    @Test
    @DisplayName("상호는 같은데 등록 안 된 지점이면 가게 불일치가 아니라 미등록 지점으로 알려준다")
    void reportsUnregisteredBranchSeparately() {
        // 사용자는 맞는 가게를 골랐다. 가게가 틀렸다고 하면 원인을 영영 못 찾는다
        givenParsed("644-40-01412", "콜드버터베이크샵 시청점");
        givenBakery(bakeryNamed("콜드버터베이크샵", MAIN_NUMBER), List.of());

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, receiptFile()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_BRANCH_NOT_REGISTERED);

        verify(receiptTokenService, never()).issueToken(anyLong(), any());
    }

    @Test
    @DisplayName("상호도 사업자번호도 다르면 가게 불일치로 막는다")
    void rejectsUnrelatedStore() {
        // 하나로마트 영수증으로 빵집을 인증할 수는 없다
        givenParsed("513-82-00249", "인동농협하나로마트");
        givenBakery(bakery(MAIN_NUMBER), List.of());

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, receiptFile()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_STORE_MISMATCH);

        verify(receiptTokenService, never()).issueToken(anyLong(), any());
    }

    @Test
    @DisplayName("사업자번호가 하나도 없는 빵집이면 가게가 없다고 하지 않고 인증 미지원으로 알려준다")
    void reportsBakeryWithoutAnyBusinessNumber() {
        // seed 에 사업자번호가 비어 있는 빵집이 17곳 있다. 빵집이 없다고 하면 원인을 못 찾는다
        givenParsed(MAIN_NUMBER, "몽심");
        givenBakery(bakery(null), List.of());

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, receiptFile()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAKERY_BUSINESS_NUMBER_MISSING);
    }

    @Test
    @DisplayName("이미 쓴 영수증이면 토큰을 발급하지 않는다")
    void rejectsAlreadyUsedReceipt() {
        givenParsed(MAIN_NUMBER, "몽심");
        givenBakery(bakery(MAIN_NUMBER), List.of());
        when(receiptHashService.create(any(), any(), any(), any())).thenReturn(HASH);
        when(receiptDuplicateChecker.isAlreadyUsed(HASH, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, receiptFile()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_ALREADY_USED);

        verify(receiptTokenService, never()).issueToken(anyLong(), any());
    }


    // ----- 픽스처 ----- //

    private void givenParsed(String businessNumber, String bakeryName) {
        when(ocrClient.extractText(any())).thenReturn("영수증 텍스트");
        when(geminiClient.parseReceipt(any())).thenReturn(new ReceiptParseResult(
                bakeryName,
                "2026-09-15",
                15800,
                "밀키연유마들렌, 에그타르트",
                RECEIPT_NUM,
                businessNumber
        ));
    }

    private void givenBakery(Bakery bakery, List<BakeryBusinessNumber> branches) {
        when(bakeryRepository.findById(STORE_ID)).thenReturn(Optional.of(bakery));
        lenient().when(bakeryBusinessNumberRepository.findAllByBakeryId(STORE_ID)).thenReturn(branches);
    }

    private void givenHashAndToken() {
        when(receiptHashService.create(any(), any(), any(), any())).thenReturn(HASH);
        when(receiptDuplicateChecker.isAlreadyUsed(HASH, USER_ID)).thenReturn(false);
        when(receiptTokenService.issueToken(eq(USER_ID), any())).thenReturn(TOKEN);
    }

    private ReceiptTokenPayload capturedPayload() {
        ArgumentCaptor<ReceiptTokenPayload> captor = ArgumentCaptor.forClass(ReceiptTokenPayload.class);
        verify(receiptTokenService).issueToken(eq(USER_ID), captor.capture());
        return captor.getValue();
    }

    private MultipartFile receiptFile() {
        return new MockMultipartFile("receipt", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private Bakery bakery(String businessNumber) {
        return bakeryNamed("몽심", businessNumber);
    }

    private Bakery bakeryNamed(String name, String businessNumber) {
        return Bakery.builder()
                .id(STORE_ID)
                .name(name)
                .region(Region.DAEDEOK)
                .businessNumber(businessNumber)
                .build();
    }

    private BakeryBusinessNumber branch(String businessNumber, Region region) {
        return BakeryBusinessNumber.builder()
                .id(1L)
                .businessNumber(businessNumber)
                .branchName("중교로점")
                .region(region)
                .build();
    }
}
