package com.bbangpatrol.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "code", "message", "data", "errors"})
public class ApiResponse<T> {
    @JsonProperty("isSuccess") // isSuccess라는 변수라는 것을 명시하는 Annotation
    private boolean isSuccess;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL) //필드 값이 null 이면 JSON 응답에서 제외됨.
    @JsonProperty("data")
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("errors")
    private final Object errors;



    //기본적으로 200 OK를 사용하는 성공 응답 생성 메서드
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, String.valueOf(HttpStatus.OK.value()), HttpStatus.OK.getReasonPhrase(), data, null);
    }

    public static ApiResponse onSuccess() {
        return new ApiResponse(true, String.valueOf(HttpStatus.NO_CONTENT.value()), HttpStatus.NO_CONTENT.getReasonPhrase(), null, null);
    }

    //응답 메시지를 직접 지정하는 성공 응답 생성 메서드
    public static <T> ApiResponse<T> onSuccess(String message, T data) {
        return new ApiResponse<>(true, String.valueOf(HttpStatus.OK.value()), message, data, null);
    }

    //상태 코드를 받아서 사용하는 성공 응답 생성 메서드
    public static <T> ApiResponse<T> onSuccess(HttpStatus status, T result) {
        return new ApiResponse<>(true, String.valueOf(status.value()), status.getReasonPhrase(), result, null);
    }

    //상태 코드와 응답 메시지를 직접 지정하는 성공 응답 생성 메서드
    public static <T> ApiResponse<T> onSuccess(HttpStatus status, String message, T data) {
        return new ApiResponse<>(true, String.valueOf(status.value()), message, data, null);
    }

    //실패 응답 생성 메서드 (상세 오류 포함)
    public static <T> ApiResponse<T> onFailure(String code, String message, Object errors) {
        return new ApiResponse<>(false, code, message, null, errors);
    }

    //실패 응답 생성 메서드 (데이터 없음)
    public static <T> ApiResponse<T> onFailure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null);
    }
}
