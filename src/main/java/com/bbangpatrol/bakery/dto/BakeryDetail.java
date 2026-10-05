package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

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
        String image,
        String summary,
        String content
) {

    // toUrl: 저장된 R2 키를 public URL 로 변환한다 (R2Service::getPublicUrl)
    public static BakeryDetail from(Bakery bakery, UnaryOperator<String> toUrl) {
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
                // 목록(BakerySummaryResponse)과 같게 대표 메뉴가 없으면 null 대신 빈 문자열
                bakery.getSignatureMenu() == null ? "" : bakery.getSignatureMenu(),
                // 사진이 없으면 "이미지 준비중입니다" 대체 이미지가 내려간다
                toUrl.apply(SignatureImage.representativeKey(bakery.getSignatureImages())),
                bakery.getSummary(),
                bakery.getContent()
        );
    }
}
