package com.bbangpatrol.bakery.dto;

public record BakeryDetailResponse(
        BakeryDetail bakery,
        long visitCnt,
        boolean likes
) {
}
