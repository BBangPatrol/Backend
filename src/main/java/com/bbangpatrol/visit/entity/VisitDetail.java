package com.bbangpatrol.visit.entity;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.review.entity.Review;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "visit_detail")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VisitDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "visited_at")
    private LocalDate visitedAt;

    @Column(name = "receipt_hash", length=64)
    private String receiptHash;

    // 영수증을 실제로 끊은 구. 지점 영수증이면 지도에 실린 구와 다르다.
    // 구별 미션은 이 값으로 센다
    @Column(length = 20)
    private Region region;

    // 나중에 지점을 별도 빵집으로 분리할 때 기존 방문을 쪼갤 근거
    @Column(name = "receipt_business_number", length = 30)
    private String receiptBusinessNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @OneToOne(mappedBy = "visitDetail", fetch = FetchType.LAZY)
    private Review review;
}
