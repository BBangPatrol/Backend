package com.bbangpatrol.bakery.dto;

import java.math.BigDecimal;

public record BakerySearchRequest(
        String sort,
        String name,
        BigDecimal lat,
        BigDecimal lon,
        Long cursor
) {

    public BakerySearchRequest {
        if (sort == null || sort.isBlank()) {
            sort = "distance";
        }
    }
}
