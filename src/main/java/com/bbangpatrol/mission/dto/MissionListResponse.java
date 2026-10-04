package com.bbangpatrol.mission.dto;

import com.bbangpatrol.common.dto.CursorPageInfo;

import java.util.List;

public record MissionListResponse(
        List<MissionResponse> missions,
        CursorPageInfo cursorPageInfo
) { }
