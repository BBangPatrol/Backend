package com.bbangpatrol.item.dto;

import java.util.List;

public record ItemListResponse(
        List<ItemResponse> items,
        Long length
) {
}
