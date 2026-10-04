package com.bbangpatrol.mission.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.mission.dto.MissionListResponse;
import com.bbangpatrol.mission.dto.MissionMainResponse;
import com.bbangpatrol.mission.dto.MissionRewardResponse;
import com.bbangpatrol.mission.service.MissionProgressService;
import com.bbangpatrol.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final MissionProgressService missionProgressService;

    // 로그인 시에만 호출 가능
    @GetMapping
    public ApiResponse<MissionListResponse> getMissions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        MissionListResponse list = missionService.getMissions(userId, filter, cursor, size);
        return ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", list);
    }

    // 메인 화면용. 노출 개수가 4개로 고정이라 filter/cursor/size 를 받지 않는다
    @GetMapping("/main")
    public ApiResponse<MissionMainResponse> getMainMissions(
            @AuthenticationPrincipal Long userId
    ) {
        MissionMainResponse main = missionService.getMainMissions(userId);
        return ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", main);
    }

    @PatchMapping("/{missionId}")
    public ApiResponse<MissionRewardResponse> receiveReward(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long missionId
    ) {
        MissionRewardResponse result = missionProgressService.receiveReward(userId, missionId);
        return ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", result);
    }
}
