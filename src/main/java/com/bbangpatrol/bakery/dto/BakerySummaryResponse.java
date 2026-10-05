package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;

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
                // 사진이 없으면 "이미지 준비중입니다" 대체 썸네일이 내려간다
                toUrl.apply(SignatureImage.representativeThumbnailKey(bakery.getSignatureImages())),
                bakery.getAvgRating(),
                bakery.getLat(),
                bakery.getLng(),
                bakery.getSignatureMenu()
        );
    }
}
