package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void simpleErrorDoesNotContainErrors() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleApiException(new ApiException(ErrorCode.NO_KAKAO_CODE));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.NO_KAKAO_CODE.getStatus());
        assertThat(response.getBody())
                .extracting("isSuccess", "code", "message", "errors")
                .containsExactly(
                        false,
                        "AUTH400",
                        "로그인 요청이 올바르지 않습니다.",
                        null
                );
    }

    @Test
    void detailedErrorContainsErrors() {
        ErrorDetail detail = new ErrorDetail("authorizationCode", "카카오 인가 코드가 비어 있습니다.");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleApiException(new ApiException(ErrorCode.NO_KAKAO_CODE, detail));

        assertThat(response.getBody())
                .extracting("errors")
                .isEqualTo(detail);
    }
}
