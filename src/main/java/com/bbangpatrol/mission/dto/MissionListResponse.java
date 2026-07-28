package com.bbangpatrol.mission.dto;

import com.bbangpatrol.common.dto.PageInfo;

import java.util.List;

public record MissionListResponse(
        List<MissionResponse> missions,
        PageInfo pageInfo
) { }
