package com.bbangpatrol.review.dto;

import com.bbangpatrol.common.dto.PageInfo;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> reviews,
        PageInfo pageInfo
) {
}
