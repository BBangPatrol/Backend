package com.bbangpatrol.mission.entity;

public enum MissionCriteria {
    RECEIPT_COUNT,      // 영수증 인증 횟수. mission.region 이 구역없음이면 전 지역
    DISTINCT_REGION,    // 인증한 서로 다른 지역 수
    DISTINCT_BAKERY,    // 방문한 서로 다른 빵집 수
    REVIEW_COUNT,       // 작성한 리뷰 수 (삭제 제외)
    DISTINCT_ITEM,      // 보유한 서로 다른 수집품 수
    NOT_SUPPORTED       // 아직 판정할 수 없는 미션 (item 에 등급/캐릭터 컬럼이 없음)
}
