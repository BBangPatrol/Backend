package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.BakeryImage;
import com.bbangpatrol.bakery.entity.SignatureImage;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public record BakeryDetail(
        Long id,
        String name,
        String region,
        String address,
        BigDecimal lat,
        BigDecimal lon,
        String phone,
        String hours,
        BigDecimal avgRating,
        String signatureMenu,
        String summary,
        String content,
        List<String> images,
        List<String> signatureImages
) {

    public static BakeryDetail from(Bakery bakery) {
        return new BakeryDetail(
                bakery.getId(),
                bakery.getName(),
                bakery.getRegion() == null ? null : bakery.getRegion().getValue(),
                bakery.getAddress(),
                bakery.getLat(),
                bakery.getLng(),
                bakery.getPhone(),
                bakery.getHours(),
                bakery.getAvgRating(),
                bakery.getSignatureMenu(),
                bakery.getSummary(),
                bakery.getContent(),
                bakery.getBakeryImages().stream()
                        .map(BakeryImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .toList(),
                bakery.getSignatureImages().stream()
                        .map(SignatureImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .toList()
        );
    }
}
