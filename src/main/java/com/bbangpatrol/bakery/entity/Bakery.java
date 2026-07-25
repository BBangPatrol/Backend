package com.bbangpatrol.bakery.entity;

import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.common.enums.Region;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bakery")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bakery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
    private Region region;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String hours;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(name = "signature_menu", length = 255)
    private String signatureMenu;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(length = 255)
    private String summary;

    @Builder.Default
    @OneToMany(mappedBy = "bakery")
    private List<Review> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "bakery")
    private List<Visit> visits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "bakery")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "bakery")
    private List<BakeryImage> bakeryImages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "bakery")
    private List<SignatureImage> signatureImages = new ArrayList<>();
}
