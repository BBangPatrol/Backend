package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.common.util.code.BaseErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

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
}
