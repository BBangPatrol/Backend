package com.bbangpatrol.bakery.dto;

import java.math.BigDecimal;

public record AttractionResponse(
        String contentId,
        String category,
        String name,
        String address,
        String imageUrl,
        BigDecimal lat,
        BigDecimal lng,
        Integer distance,
        String tel
) {}
