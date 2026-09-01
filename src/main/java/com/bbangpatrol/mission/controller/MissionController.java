package com.bbangpatrol.mission.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.mission.dto.MissionListResponse;
import com.bbangpatrol.mission.dto.MissionRewardResponse;
import com.bbangpatrol.mission.service.MissionProgressService;
import com.bbangpatrol.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
//            @AuthenticationPrincipal Long userId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) Long cursor
    ) {
        MissionListResponse list = missionService.getMissions(userId, filter);
        return ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", list);
    }

    @PatchMapping("/{missionId}")
    public ApiResponse<MissionRewardResponse> receiveReward(
//            @AuthenticationPrincipal Long userId,
            @RequestParam Long userId,
            @PathVariable Long missionId
    ) {
        MissionRewardResponse result = missionProgressService.receiveReward(userId, missionId);
        return ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", result);
    }
}
