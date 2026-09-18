package com.bbangpatrol.demo.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.demo.dto.PreviewReceiptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 코드가 틀렸는데도 URL 이 나가면 시연용 영수증이 사실상 공개된 것과 같다.
 */
@ExtendWith(MockitoExtension.class)
class DemoPreviewServiceTest {

    private static final long USER_ID = 6L;
    private static final String KEY = "preview/preview_receipt.jpg";

    @Mock
    private R2Service r2Service;

    @InjectMocks
    private DemoPreviewService demoPreviewService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoPreviewService, "previewCode", "BBANGBEOM");
        ReflectionTestUtils.setField(demoPreviewService, "receiptKey", KEY);
        lenient().when(r2Service.getPublicUrl(KEY)).thenReturn("https://cdn.test/" + KEY);
    }

    @Test
    @DisplayName("코드가 맞으면 사진 URL 과 파일 이름을 준다")
    void 코드가_맞으면_사진을_준다() {
        PreviewReceiptResponse response = demoPreviewService.getPreviewReceipt(USER_ID, "BBANGBEOM");

        assertThat(response.imageUrl()).isEqualTo("https://cdn.test/" + KEY);
        // 확장자가 빠지면 클라이언트가 다시 올릴 때 R2Service 확장자 검증에 걸린다
        assertThat(response.fileName()).isEqualTo("preview_receipt.jpg");
    }

    @Test
    @DisplayName("폰으로 친 코드라 앞뒤 공백과 대소문자는 통과시킨다")
    void 공백과_대소문자는_허용한다() {
        assertThat(demoPreviewService.getPreviewReceipt(USER_ID, "  bbangbeom ").imageUrl()).isNotNull();
    }

    @Test
    @DisplayName("코드가 틀리면 403 이고 URL 은 만들지 않는다")
    void 코드가_틀리면_거절한다() {
        assertThatThrownBy(() -> demoPreviewService.getPreviewReceipt(USER_ID, "BBANG"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEMO_CODE);

        verify(r2Service, never()).getPublicUrl(KEY);
    }

    @Test
    @DisplayName("코드가 null 이어도 NPE 가 아니라 403 이다")
    void 코드가_없으면_거절한다() {
        assertThatThrownBy(() -> demoPreviewService.getPreviewReceipt(USER_ID, null))
                .isInstanceOf(ApiException.class);
    }
}
