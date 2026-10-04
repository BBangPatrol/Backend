package com.bbangpatrol.item.dto;

import com.bbangpatrol.item.entity.ItemRank;

public record DrawResultResponse(
        Long collectibleId,
        String name,
        ItemRank rank,
        String image,
        boolean duplicated,
        int refundPoint,    // 중복이면 환급액, 아니면 0
        Integer currentPoint
) {

}
