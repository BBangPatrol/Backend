package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.bbangpatrol.visit.service.ReceiptDuplicateChecker;
import com.bbangpatrol.visit.service.ReceiptHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 실제 영수증으로 확인한 판정을 고정한다.
 *
 * 가이드북에 실린 것은 그 매장 한 곳이므로, 같은 브랜드라도 다른 지점 영수증은 인정하지 않는다.
 * 다만 사용자가 할 수 있는 일이 "지점을 다시 고르는 것"이므로 일반 불일치와 다른 문구를 준다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OcrServiceVerifyStoreTest {

    private static final long USER_ID = 6L;
    private static final long STORE_ID = 1L;

    @Mock
    private ReceiptImageValidator imageValidator;
    @Mock
    private OcrClient ocrClient;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private ReceiptTokenService receiptTokenService;
    @Mock
    private ReceiptHashService receiptHashService;
    @Mock
    private ReceiptDuplicateChecker receiptDuplicateChecker;

    private OcrServiceImpl ocrService;

    @BeforeEach
    void setUp() {
        // 매처는 진짜를 쓴다. 판정 규칙이 이 테스트의 대상이다
        ReceiptStoreMatcher matcher = new ReceiptStoreMatcher();
        ocrService = new OcrServiceImpl(imageValidator, ocrClient, geminiClient, bakeryRepository,
                matcher, new StoreResolver(bakeryRepository, matcher),
                receiptTokenService, receiptHashService, receiptDuplicateChecker);

        when(ocrClient.extractText(any())).thenReturn("ocr-text");
        when(receiptHashService.create(any(), any(), any(), any())).thenReturn("hash");
        when(receiptDuplicateChecker.isAlreadyUsed(anyString(), anyLong())).thenReturn(false);
        when(receiptTokenService.issueToken(any())).thenReturn("token");
    }

    @Test
    @DisplayName("사업자번호와 주소가 맞으면 통과한다 (슬로우브레드 영수증)")
    void passesWhenNumberAndAddressMatch() {
        givenBakery("슬로우브레드", "대전광역시 유성구 유성대로 1734-1, 1층", "314-22-67770");
        givenReceipt("슬로우브레드", "대전 유성구 유성대로 1734-1 (전민동)", "314-22-67770");

        OcrResponse response = ocrService.getInfo(USER_ID, STORE_ID, image());

        assertThat(response.verificationToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("상호는 같은데 사업자번호가 다르면 지점 안내를 낸다 (콜드버터베이크샵 시청점 영수증)")
    void branchMismatchWhenNumberDiffersButNameMatches() {
        givenBakery("콜드버터베이크샵", "대전광역시 중구 중앙로 112번길 37, 1층", "793-59-00304");
        givenReceipt("콜드버터베이크샵 시청점", "대전 서구 둔산중로64번길 33, 1층 일부호", "644-40-01412");

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, image()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH);
    }

    @Test
    @DisplayName("사업자번호가 같아도 주소가 다르면 지점 안내를 낸다 (한 법인이 여러 지점을 쓰는 경우)")
    void branchMismatchWhenAddressDiffers() {
        givenBakery("성심당", "대전광역시 중구 대종로 480번길 15", "305-81-48738");
        givenReceipt("성심당 대전역점", "대전 동구 중앙로 215", "305-81-48738");

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, image()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH);
    }

    @Test
    @DisplayName("상호도 번호도 다르면 일반 불일치다 (빵집이 아닌 영수증)")
    void storeMismatchWhenNothingMatches() {
        givenBakery("슬로우브레드", "대전광역시 유성구 유성대로 1734-1, 1층", "314-22-67770");
        givenReceipt("인동농협하나로마트", "경북 구미시 인동남길 106", "513-82-00249");

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, image()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_STORE_MISMATCH);
    }

    @Test
    @DisplayName("사업자번호를 모르는 가게는 상호와 주소가 모두 맞아야 통과한다")
    void passesUnknownNumberStoreWithNameAndAddress() {
        givenBakery("연이가 베이크샵", "대전광역시 서구 둔산남로 175번길 10, 102호", null);
        givenReceipt("연이가", "대전 서구 둔산남로175번길 10", "123-45-67890");

        OcrResponse response = ocrService.getInfo(USER_ID, STORE_ID, image());

        assertThat(response.verificationToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("사업자번호를 모르는 가게도 주소가 다르면 지점 안내를 낸다")
    void branchMismatchForUnknownNumberStore() {
        givenBakery("연이가 베이크샵", "대전광역시 서구 둔산남로 175번길 10, 102호", null);
        givenReceipt("연이가", "대전 유성구 봉명로 12", "123-45-67890");

        assertThatThrownBy(() -> ocrService.getInfo(USER_ID, STORE_ID, image()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH);
    }

    private void givenBakery(String name, String address, String businessNumber) {
        Bakery bakery = Bakery.builder()
                .id(STORE_ID)
                .name(name)
                .address(address)
                .businessNumber(businessNumber)
                .build();
        when(bakeryRepository.findByIdAndDeletedAtIsNull(STORE_ID)).thenReturn(Optional.of(bakery));
    }

    private void givenReceipt(String name, String address, String businessNumber) {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
        when(geminiClient.parseReceipt(anyString())).thenReturn(new ReceiptParseResult(
                name, address, today, 11300, "소금빵", "68719332", businessNumber));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("receipt", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
