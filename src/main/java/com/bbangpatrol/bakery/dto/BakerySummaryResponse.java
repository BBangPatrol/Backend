package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.BakeryImage;
import com.bbangpatrol.bakery.entity.SignatureImage;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public record BakerySummaryResponse(
        Long id,
        String name,
        String image,
        BigDecimal avgRating,
        BigDecimal lat,
        BigDecimal lon,
        String signatureMenu,
        List<String> signatureImages
) {

    public static BakerySummaryResponse from(Bakery bakery) {
        String image = bakery.getBakeryImages().stream()
                .map(BakeryImage::getImageUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        List<String> signatureImages = bakery.getSignatureImages().stream()
                .map(SignatureImage::getImageUrl)
                .filter(StringUtils::hasText)
                .toList();

        return new BakerySummaryResponse(
                bakery.getId(),
                bakery.getName(),
                image,
                bakery.getAvgRating(),
                bakery.getLat(),
                bakery.getLng(),
                bakery.getSignatureMenu(),
                signatureImages
        );
    }
}
