package com.bbangpatrol.item.entity;

// 표기는 커먼 / 레어 / 에픽 / 레전더리로 통일한다.
// 상수 이름은 item.rank 에 그대로 저장돼 있어 바꾸려면 마이그레이션이 필요하므로 NORMAL 로 둔다.
public enum ItemRank {
    NORMAL,     // 커먼
    RARE,       // 레어
    EPIC,       // 에픽
    LEGENDARY   // 레전더리
}
