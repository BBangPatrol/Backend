package com.bbangpatrol.util.exception;

import com.bbangpatrol.util.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public ApiException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
