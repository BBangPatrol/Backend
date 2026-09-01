package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.BaseErrorCode;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR_500;
        log.error("[DB] 처리되지 않은 데이터 접근 예외", exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }
}
