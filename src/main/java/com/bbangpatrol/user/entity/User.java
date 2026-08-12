package com.bbangpatrol.user.entity;

import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.PointHistory;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import com.bbangpatrol.visit.entity.Visit;
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

    @Column(nullable = false, length = 50) // 이름이 중복될 수도 있어서 Unique 제약 삭제
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

    public static User ofKakao(Long kakaoId, String email, String name) {
        return User.builder()
                .name(name)                       // 카카오에서 가져온 이름 바로 사용
                .email(email)
                .kakaoId(String.valueOf(kakaoId)) // Long에서 String 변환
                .rank(1)                          // 초기 등급
                .pointBalance(0)                  // 초기 포인트
                .role(UserRole.USER)              // 역할은 USER로 고정
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void addPoint(int point) {
        this.pointBalance += point;
    }

    public void updateNickname(String nickname) { this.name = nickname; }
}
