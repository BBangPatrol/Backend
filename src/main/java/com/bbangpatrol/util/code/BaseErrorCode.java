package com.bbangpatrol.util.code;

import com.bbangpatrol.util.ApiResponse;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();

    default ApiResponse<Void> getErrorResponse() {
        return ApiResponse.onFailure(getCode(), getMessage());
    }

}
