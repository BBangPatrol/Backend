package com.bbangpatrol.common.util.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode implements BaseErrorCode{

    // Auth에 관한 에러들
    NO_KAKAO_CODE(HttpStatus.BAD_REQUEST, "AUTH400", "로그인 요청이 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "리프레시 토큰이 유효하지 않거나 만료되었습니다."),

    // OCR에 관한 에러들
    NO_IMAGE_ATTACHED(HttpStatus.BAD_REQUEST, "OCR400", "유효하지 않는 요청입니다."),
    TOO_LARGE_PAYLOAD(HttpStatus.PAYLOAD_TOO_LARGE, "OCR413", "이미지가 너무 큽니다(최대 10MB)."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "OCR415", "지원하지 않는 이미지 형식입니다."),
    OCR_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "OCR001", "영수증 이미지를 처리할 수 없습니다"),
    OCR_EMPTY_RESULT(HttpStatus.UNPROCESSABLE_ENTITY, "OCR002", "영수증에서 텍스트를 찾지 못했습니다"),
    OCR_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "OCR003", "OCR 서버에 문제가 발생했습니다"),
    OCR_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "OCR004", "OCR 서버에 연결할 수 없습니다"),
    IMAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OCR005", "이미지를 읽을 수 없습니다"),

    // OCR 이후 파싱에 관한 에러들
    RECEIPT_PARSE_FAILED(HttpStatus.BAD_REQUEST, "PARSE400-1", "OCR 파싱 중 문제가 발생했습니다."),
    RECEIPT_BAKERY_NAME_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-2", "영수증에서 상호명이 인식되지 않았습니다."),
    RECEIPT_DATE_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-3", "영수증에서 날짜가 인식되지 않았습니다."),
    RECEIPT_AMOUNT_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-4", "영수증에서 금액이 인식되지 않았습니다."),
    RECEIPT_MENU_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-5", "영수증에서 메뉴가 인식되지 않았습니다."),
    RECEIPT_NUM_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-6", "영수증에서 승인번호가 인식되지 않았습니다."),
    RECEIPT_TOO_OLD(HttpStatus.BAD_REQUEST, "PARSE400-7", "영수증의 등록 기한이 만료되었습니다."),
    RECEIPT_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "PARSE400-8", "영수증과 인증 가게가 일치하지 않습니다."),
    RECEIPT_BUSINESS_NUMBER_MISSING(HttpStatus.BAD_REQUEST, "PARSE400-9", "영수증에서 사업자번호가 인식되지 않았습니다."),

    // 빵집 관련 에러들
    RECEIPT_ALREADY_USED(HttpStatus.CONFLICT, "BAKE002", "이미 사용한 영수증입니다."),
    INVALID_RECEIPT_TOKEN(HttpStatus.BAD_REQUEST, "BAKE003", "유효하지 않거나 만료된 인증 토큰입니다."),
    BAKERY_NOT_FOUND(HttpStatus.NOT_FOUND, "BAKERY404" , "빵집이 존재하지 않습니다." ),
    ATTRACTION_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "BAKERY502", "근처 관광지 정보를 가져오지 못했습니다."),

    // Mission에 관한 에러
    MISSION_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "존재하지 않는 미션 번호입니다."),
    ALREADY_REWARDED(HttpStatus.BAD_REQUEST, "MISSION400", "이미 보상을 수령한 미션입니다."),
    MISSION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "MISSION401", "아직 완료되지 않은 미션입니다."),

    // user에 관한 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자가 존재하지 않습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER409", "닉네임이 중복됩니다."),
    USER_UNAUTHORIZE(HttpStatus.FORBIDDEN, "USER403" , "권한이 없습니다."),

    // 리뷰 에러,
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404", "리뷰가 존재하지 않습니다."),
    VISIT_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "REVIEW400", "리뷰를 작성하려면 영수증 인증이 필요합니다."),

    // R2 에러,
    R2_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "R2500" , "이미지 I/O 요청이 실패했습니다." ),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "POINT400", "포인트가 부족합니다."),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED_401(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다"),
    FORBIDDEN_403(HttpStatus.FORBIDDEN, "COMMON403", "접근이 금지되었습니다"),
    NOT_FOUND_404(HttpStatus.NOT_FOUND, "COMMON404", "요청한 자원을 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED_405(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "허용되지 않은 HTTP 메서드입니다"),
    EXTERNAL_SERVER_ERROR_502(HttpStatus.BAD_GATEWAY, "COMMON502", "외부 서버 응답에 문제가 발생했습니다"),
    UNSUPPORTED_MEDIA_TYPE415(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON415", "지원하지 않는 파일형식입니다."),
    UPLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "COMMON413", "업로드 용량이 너무 큽니다. 사진은 장당 15MB, 한 번에 60MB 까지 가능합니다."),
    INTERNAL_SERVER_ERROR_500(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
