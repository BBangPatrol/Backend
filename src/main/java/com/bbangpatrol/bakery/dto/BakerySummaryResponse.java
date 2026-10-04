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
                // 빵집당 시그니처 사진 한 장을 대표 이미지로 사용한다.
                // 목록은 빵집 20개를 한 번에 받으므로 원본(평균 610KB) 대신 썸네일(약 28KB)을 쓴다.
                // 썸네일이 없는 행은 원본으로 폴백한다
                bakery.getSignatureImages().stream()
                        .map(BakerySummaryResponse::listImageKey)
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

    private static String listImageKey(SignatureImage image) {
        return StringUtils.hasText(image.getThumbnailUrl()) ? image.getThumbnailUrl() : image.getImageUrl();
    }
}
