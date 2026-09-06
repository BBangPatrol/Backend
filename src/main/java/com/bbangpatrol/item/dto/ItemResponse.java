package com.bbangpatrol.item.dto;

import com.bbangpatrol.item.entity.ItemRank;

public record ItemResponse(
        Long collectibleId,
        String name,
        ItemRank rank,
        String image
) {
}
