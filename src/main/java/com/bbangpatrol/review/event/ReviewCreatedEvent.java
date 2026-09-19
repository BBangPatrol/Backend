package com.bbangpatrol.review.event;

import com.bbangpatrol.common.enums.Region;

/** 리뷰 작성이 확정(커밋)된 뒤에 보내는 신호. 미션 갱신을 리뷰 트랜잭션에서 떼어내기 위해 쓴다. */
public record ReviewCreatedEvent(Long userId, Region region) {
}
