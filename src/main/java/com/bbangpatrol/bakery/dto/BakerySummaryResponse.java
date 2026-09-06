package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

public record BakerySummaryResponse(
        Long id,
        String name,
        String image,
        BigDecimal avgRating,
        BigDecimal lat,
        BigDecimal lon,
        String signatureMenu
) {

    // toUrl: 저장된 R2 키를 public URL 로 변환한다 (R2Service::getPublicUrl)
    public static BakerySummaryResponse from(Bakery bakery, UnaryOperator<String> toUrl) {
        return new BakerySummaryResponse(
                bakery.getId(),
                bakery.getName(),
                // 빵집당 시그니처 사진 한 장을 대표 이미지로 사용한다
                bakery.getSignatureImages().stream()
                        .map(SignatureImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .map(toUrl)
                        .orElse(null),
                bakery.getAvgRating(),
                bakery.getLat(),
                bakery.getLng(),
                bakery.getSignatureMenu()
        );
    }
}
