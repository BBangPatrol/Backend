package com.bbangpatrol.common.util.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode implements BaseErrorCode{

    // Auth에 관한 에러들
    NO_KAKAO_CODE(HttpStatus.BAD_REQUEST, "AUTH400", "로그인 요청이 올바르지 않습니다."),

    // OCR에 관한 에러들
    NO_IMAGE_ATTACHED(HttpStatus.BAD_REQUEST, "OCR400", "유효하지 않는 요청입니다."),
    TOO_LARGE_PAYLOAD(HttpStatus.PAYLOAD_TOO_LARGE, "OCR413", "이미지가 너무 큽니다(최대 10MB)."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "OCR415", "지원하지 않는 이미지 형식입니다."),
    OCR_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "OCR001", "영수증 이미지를 처리할 수 없습니다"),
    OCR_EMPTY_RESULT(HttpStatus.UNPROCESSABLE_ENTITY, "OCR002", "영수증에서 텍스트를 찾지 못했습니다"),
    OCR_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "OCR003", "OCR 서버에 문제가 발생했습니다"),
    OCR_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "OCR004", "OCR 서버에 연결할 수 없습니다"),
    IMAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OCR005", "이미지를 읽을 수 없습니다"),

    // Mission에 관한 에러
    MISSION_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "존재하지 않는 미션 번호입니다."),
    ALREADY_REWARDED(HttpStatus.BAD_REQUEST, "MISSION400", "이미 보상을 수령한 미션입니다."),
    MISSION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "MISSION401", "아직 완료되지 않은 미션입니다."),

    // user에 관한 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자가 존재하지 않습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER409", "닉네임이 중복됩니다."),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED_401(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다"),
    FORBIDDEN_403(HttpStatus.FORBIDDEN, "COMMON403", "접근이 금지되었습니다"),
    NOT_FOUND_404(HttpStatus.NOT_FOUND, "COMMON404", "요청한 자원을 찾을 수 없습니다"),
    UNSUPPORTED_MEDIA_TYPE415(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMO405", "지원하지 않는 파일형식입니다."),
    INTERNAL_SERVER_ERROR_500(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}