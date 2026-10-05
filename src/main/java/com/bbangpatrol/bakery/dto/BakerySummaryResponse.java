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
                // 프론트가 signatureMenu.split(",") 으로 태그를 만든다. 대표 메뉴가 없는 빵집(V20 축제 시드 일부)은
                // null 대신 빈 문자열로 내려 지도 팝업/카드가 깨지지 않게 한다
                bakery.getSignatureMenu() == null ? "" : bakery.getSignatureMenu()
        );
    }
}
