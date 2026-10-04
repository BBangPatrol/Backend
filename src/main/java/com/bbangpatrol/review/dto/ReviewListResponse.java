package com.bbangpatrol.review.dto;

import com.bbangpatrol.common.dto.OffsetPageInfo;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> reviews,
        long count,
        OffsetPageInfo pageInfo
) {
}
