package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.common.dto.PageInfo;

import java.util.List;

public record BakerySearchResponse(
        List<BakerySearchItemResponse> result,
        PageInfo pageInfo
) {
}
