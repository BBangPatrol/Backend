package com.bbangpatrol.user.entity;

import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.PointHistory;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(name = "kakao_id", nullable = false, length = 100)
    private String kakaoId;

    @Column(name = "`rank`", nullable = false)
    private Integer rank;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private UserRole role;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "user")
    private UserImage userImage;

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Review> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Visit> visits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<MissionProgress> missionProgresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<PointHistory> pointHistories = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ReviewLike> reviewLikes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<UserItem> userItems = new ArrayList<>();
}
