package com.bbangpatrol.bakery.dto;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;
import org.springframework.util.StringUtils;

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
                bakery.getSignatureMenu(),
                // 빵집당 시그니처 사진 한 장을 대표 이미지로 사용한다
                bakery.getSignatureImages().stream()
                        .map(SignatureImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .map(toUrl)
                        .orElse(null),
                bakery.getSummary(),
                bakery.getContent()
        );
    }
}
