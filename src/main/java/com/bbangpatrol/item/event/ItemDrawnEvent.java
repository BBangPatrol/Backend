package com.bbangpatrol.item.event;

/** 수집품 뽑기가 확정(커밋)된 뒤에 보내는 신호. 미션 갱신을 뽑기 트랜잭션에서 떼어내기 위해 쓴다. */
public record ItemDrawnEvent(Long userId) {
}
