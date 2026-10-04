package com.bbangpatrol.mission.dto;

import java.util.List;

/**
 * 메인 화면용 미션 응답. 노출 개수가 고정이라 pageInfo 를 두지 않는다.
 * 항목 스키마는 목록과 동일하므로 FE 는 같은 파서를 그대로 쓸 수 있다.
 */
public record MissionMainResponse(
        List<MissionResponse> missions
) { }
