package com.bbangpatrol.bakery.entity;

import com.bbangpatrol.common.enums.Region;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 한 빵집으로 인정할 사업자번호. 빵산책 지도가 브랜드당 한 매장만 실어
 * 지점 영수증이 전부 막히는 문제를 여기서 푼다.
 */
@Entity
@Table(name = "bakery_business_number")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BakeryBusinessNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id")
    private Bakery bakery;

    @Column(name = "business_number", nullable = false, length = 30)
    private String businessNumber;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    // 지점이 실제로 위치한 구. null 이면 빵집 본체의 구를 따른다
    @Column(length = 20)
    private Region region;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
