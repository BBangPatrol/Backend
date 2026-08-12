package com.bbangpatrol.common.exception;

import com.bbangpatrol.common.util.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final ErrorDetail errors;

    public ApiException(BaseErrorCode errorCode) {
        this(errorCode, null);
    }

    public ApiException(BaseErrorCode errorCode, ErrorDetail errors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errors = errors;
    }
}
