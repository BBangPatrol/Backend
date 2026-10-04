package com.bbangpatrol.review.entity;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.visit.entity.VisitDetail;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;

    @Lob private String content;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id")
    private Bakery bakery;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_detail_id", unique = true)
    private VisitDetail visitDetail;

    @Builder.Default
    @OneToMany(mappedBy = "review")
    private List<ReviewImage> reviewImages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "review")
    private List<ReviewKeyword> reviewKeywords = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "review")
    private List<ReviewLike> reviewLikes = new ArrayList<>();

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        // UNIQUE 제약 때문에 끊어 줘야 같은 방문에 다시 쓸 수 있다
        this.visitDetail = null;
    }

    public void decreaseLikeCount() {
        this.likeCount -= 1;
    }

    public void increaseLikeCount() {
        this.likeCount += 1;
    }
}
