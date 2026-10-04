package com.bbangpatrol.item.entity;

// 한글 표기는 일반 / 희귀 / 영웅 / 전설 이다 (수집품 기획서와 도감 배지 기준, V17).
// 여기 상수 이름은 item.rank 에 그대로 저장돼 있고 클라이언트에도 이 이름으로 나간다.
// 한글 라벨은 프론트의 배지(COLLECTIBLE_RANK_STYLES)가 붙이므로 서버는 이름만 관리한다.
public enum ItemRank {
    NORMAL,     // 일반
    RARE,       // 희귀
    EPIC,       // 영웅
    LEGENDARY   // 전설
}
