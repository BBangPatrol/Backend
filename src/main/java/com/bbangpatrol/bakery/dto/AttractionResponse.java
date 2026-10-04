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
        String tel,
        /** TourAPI 이미지 저작권 유형 (Type1 = 공공누리 제1유형, Type3 = 제3유형 변경금지) */
        String copyrightType
) {}
