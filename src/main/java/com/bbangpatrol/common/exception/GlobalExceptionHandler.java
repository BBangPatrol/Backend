package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.BaseErrorCode;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR_500;
        log.error("[DB] 처리되지 않은 데이터 접근 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }
}
