package com.bbangpatrol.bakery.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
