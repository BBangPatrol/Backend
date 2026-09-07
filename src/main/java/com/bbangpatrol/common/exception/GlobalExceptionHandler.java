package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.BaseErrorCode;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(IOException.class)
    public ApiResponse handleIOException(IOException exception) {
        return ApiResponse.onFailure("IOEXCEPTION", exception.getMessage());
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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR_500;
        log.error("[DB] 처리되지 않은 데이터 접근 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }
}
