package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.visit.dto.VisitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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
    void validationErrorUsesApiResponseShape() throws Exception {
        // @Valid 로 걸러진 요청도 다른 에러와 같은 형태로 나가야 한다
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new VisitRequest(), "visitRequest");
        bindingResult.addError(new FieldError("visitRequest", "date", "널이어서는 안됩니다"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", VisitRequest.class), 0);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(
                new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.BAD_REQUEST.getStatus());
        assertThat(response.getBody())
                .extracting("isSuccess", "code", "errors")
                .containsExactly(false, "COMMON400", new ErrorDetail("date", "널이어서는 안됩니다"));
    }

    @SuppressWarnings("unused")
    private void dummy(VisitRequest request) {
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
    @Test
    void unreadableBodyIsFourHundredAndHidesInternals() {
        // 예전에는 Jackson 예외의 원인이 IOException 이라 handleIOException 이 잡았고,
        // 그 핸들러는 ResponseEntity 없이 반환해서 실패인데도 HTTP 200 이 나갔다
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableRequest(
                new HttpMessageNotReadableException("Unexpected end-of-input", (org.springframework.http.HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .extracting("isSuccess", "code", "message")
                .containsExactly(false, "COMMON400", "잘못된 요청입니다.");
    }

    @Test
    void serverSideIoFailureIsFiveHundred() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIOException(new java.io.IOException("R2 upload failed"));

        // 이전에는 200 이 나갔다
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).extracting("code").isEqualTo("R2500");
    }

    @Test
    void missingParameterIsFourHundredWithFieldName() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("type", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .extracting("errors")
                .isEqualTo(new ErrorDetail("type", "필수 파라미터가 없습니다."));
    }

    @Test
    void unmappedUrlIsFourOhFourInApiResponseShape() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).extracting("code").isEqualTo("COMMON404");
    }

    @Test
    void wrongMethodIsFourOhFiveInApiResponseShape() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).extracting("code").isEqualTo("COMMON405");
    }

    @Test
    void unsupportedMediaTypeIsFourFifteenInApiResponseShape() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("application/xml"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).extracting("code").isEqualTo("COMMON415");
    }

    @Test
    void externalApiFailureKeepsStatusAndUsesApiResponseShape() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody())
                .extracting("isSuccess", "code", "message")
                .containsExactly(false, "COMMON502", "카카오 사용자 정보 조회에 실패했습니다.");
    }

    @Test
    void externalApiFailureFallsBackToErrorCodeMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).extracting("code").isEqualTo("COMMON400");
    }
}
