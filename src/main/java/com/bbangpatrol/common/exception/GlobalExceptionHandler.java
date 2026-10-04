package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.BaseErrorCode;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        BaseErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        exception.getErrors()
                ));
    }

    // 전용 핸들러가 없으면 원인인 Jackson 예외(= IOException)를 handleIOException 이 잡아 200 이 나간다
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        // Jackson 메시지에는 내부 클래스명이 들어 있어 그대로 내보내지 않는다
        log.warn("요청 본문을 읽을 수 없음: {}", exception.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        new ErrorDetail(exception.getParameterName(), "필수 파라미터가 없습니다.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        new ErrorDetail(exception.getName(), "값의 형식이 올바르지 않습니다.")));
    }

    // 여기까지 온 IOException 은 R2 업로드 같은 서버 쪽 실패다
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException exception) {
        ErrorCode errorCode = ErrorCode.R2_IO_ERROR;
        log.error("[IO] 처리되지 않은 입출력 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    // 한도 초과 업로드는 핸들러가 없으면 500 이 나간다. 사용자가 원인을 알 수 있게 413 으로 내려준다
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        ErrorCode errorCode = ErrorCode.UPLOAD_TOO_LARGE;
        log.warn("업로드 용량 초과: {}", exception.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * @ModelAttribute 바인딩·검증 실패. multipart 요청은 MethodArgumentNotValidException 대신
     * 이 예외가 올라와 그대로 두면 아래 handleException 이 500 으로 받아 버린다.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        FieldError fieldError = exception.getBindingResult().getFieldError();
        ErrorDetail detail = fieldError == null
                ? null
                : new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        FieldError fieldError = exception.getBindingResult().getFieldError();
        ErrorDetail detail = fieldError == null
                ? null
                : new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), detail));
    }

    // 없는 주소. 상태는 404 로 나가지만 핸들러가 없으면 본문이 스프링 기본 형식이다
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.NOT_FOUND_404;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED_405;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE415;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    // 외부 API 호출부가 던지는 ResponseStatusException. 상태 코드는 그대로 두고 본문만 ApiResponse 로 맞춘다
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        ErrorCode errorCode = switch (exception.getStatusCode().value()) {
            case 400 -> ErrorCode.BAD_REQUEST;
            case 401 -> ErrorCode.UNAUTHORIZED_401;
            case 403 -> ErrorCode.FORBIDDEN_403;
            case 404 -> ErrorCode.NOT_FOUND_404;
            case 502 -> ErrorCode.EXTERNAL_SERVER_ERROR_502;
            default -> ErrorCode.INTERNAL_SERVER_ERROR_500;
        };
        // reason 은 우리가 직접 적은 문구라 그대로 내보내도 된다
        String message = StringUtils.hasText(exception.getReason()) ? exception.getReason() : errorCode.getMessage();
        log.warn("[ResponseStatusException] {} {}", exception.getStatusCode(), exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(ApiResponse.onFailure(errorCode.getCode(), message));
    }

    // 외부 API(TourAPI·카카오·Gemini·OCR) 호출이 실패해 여기까지 온 경우.
    //
    // 전용 핸들러를 두지 않으면 Spring 이 cause 사슬을 따라가며 핸들러를 찾는다.
    // RestClient 가 응답 본문을 못 읽으면 cause 가 HttpMessageNotReadableException 이라
    // 요청 파싱용으로 만든 handleUnreadableRequest 가 대신 잡아서, 서버 쪽 문제가
    // 400(잘못된 요청)으로 둔갑해 나간다. 원인이 클라이언트에 있는 것처럼 보이게 된다.
    //
    // 관광지처럼 전용 코드가 있는 호출은 각 클라이언트가 ApiException 으로 바꿔 던지므로
    // 여기는 그러지 않은 나머지를 받는 그물이다.
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApiException(RestClientException exception) {
        ErrorCode errorCode = ErrorCode.EXTERNAL_SERVER_ERROR_502;
        log.error("[외부 API] 처리되지 않은 호출 실패", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR_500;
        log.error("[DB] 처리되지 않은 데이터 접근 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 위에서 못 잡은 예외. 이게 없으면 Spring 기본 500 이 나가 프론트가 message 를 읽을 수 없다.
     * 예) 시그니처 사진 없는 빵집에서 터졌던 /stores/hot 의 NPE
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR_500;
        log.error("[Exception] 처리하지 못한 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), null));
    }
}
