package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.common.dto.CursorPageInfo;

import java.util.List;

public record BakerySearchResponse(
        List<BakerySearchItemResponse> result,
        CursorPageInfo cursorPageInfo
) {
}
