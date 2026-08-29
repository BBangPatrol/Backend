package com.bbangpatrol.bakery.dto;

public record BakerySearchItemResponse(
        BakerySummaryResponse bakery,
        long visitCnt,
        boolean likes
) {
}
