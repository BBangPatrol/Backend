package com.bbangpatrol.ocr.dto;

import com.bbangpatrol.common.enums.Region;

/**
 * 영수증 인증 토큰에 실어 나르는 값.
 * businessNumber 는 DB 대표번호가 아니라 영수증에 찍힌 번호라야
 * 지점이 다른 영수증의 해시가 서로 겹치지 않는다.
 * region 은 실제로 방문한 구라서 미션 집계 기준이 된다.
 */
public record ReceiptTokenPayload(
        String receiptNum,
        String businessNumber,
        Region region
) {

}
