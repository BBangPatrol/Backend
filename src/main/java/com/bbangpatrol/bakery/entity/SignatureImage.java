package com.bbangpatrol.bakery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.List;

@Entity
@Table(name = "sig_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SignatureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob private String origin;

    @Lob
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** 목록 조회용 썸네일 key. null 이면 썸네일이 없다는 뜻이고 조회 시 원본으로 폴백한다 */
    @Lob
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bakery_id")
    private Bakery bakery;

    /**
     * 시그니처 사진이 없는 빵집(V20 축제 시드 등)에 내려줄 "이미지 준비중입니다" 대체 이미지.
     * R2 에 다른 빵집 사진과 같은 규칙(bakeries/{id}/signature_menu.jpg)으로 올라가 있다.
     * 사진이 생기면 sig_image 에 행만 넣으면 되고 코드는 바뀌지 않는다.
     */
    public static final String PLACEHOLDER_KEY = "bakeries/placeholder/signature_menu.jpg";
    public static final String PLACEHOLDER_THUMBNAIL_KEY = "bakeries/placeholder/signature_menu_thumb.jpg";

    /** 빵집당 시그니처 사진 한 장을 대표 이미지로 쓴다. 없으면 대체 이미지 */
    public static String representativeKey(List<SignatureImage> images) {
        return images.stream()
                .map(SignatureImage::getImageUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(PLACEHOLDER_KEY);
    }

    /**
     * 목록용. 썸네일이 없는 행은 원본으로, 사진 자체가 없으면 대체 썸네일로 폴백한다.
     * (목록은 빵집 20개를 한 번에 받으므로 원본 대신 썸네일을 쓴다)
     */
    public static String representativeThumbnailKey(List<SignatureImage> images) {
        return images.stream()
                .map(img -> StringUtils.hasText(img.getThumbnailUrl()) ? img.getThumbnailUrl() : img.getImageUrl())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(PLACEHOLDER_THUMBNAIL_KEY);
    }
}
