package com.bbangpatrol.visit.event;

import com.bbangpatrol.common.enums.Region;

/**
 * 영수증 인증이 확정(커밋)된 뒤에 보내는 신호.
 * 미션 갱신처럼 "인증이 끝난 다음에 하면 되는 일"을 인증 트랜잭션에서 떼어내기 위해 쓴다.
 */
public record ReceiptVerifiedEvent(Long userId, Region region) {
}
