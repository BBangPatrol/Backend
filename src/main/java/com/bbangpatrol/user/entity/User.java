package com.bbangpatrol.user.entity;

import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.Point;
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

    @Column(name = "user_image")
    private String userImage;

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
    private List<Point> pointHistories = new ArrayList<>();

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

    public void updateImage(String key) { this.userImage = key; }

    /**
     * 탈퇴. 행을 바로 지우지 않고 표시만 남긴다 —
     * 개인정보처리방침이 약속한 보관 기간(30일)이 지나면 AccountPurgeService 가 실제로 지운다.
     * 리프레시 토큰을 함께 비워야 탈퇴 후 토큰 재발급으로 되살아나지 않는다.
     */
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.refreshToken = null;
    }

    public void usePoint(int point) { // 포인트 사용했을 경우 감소 처리

        if (this.pointBalance < point) { // 보유 포인트가 부족하면 에러 발생
            throw new ApiException(ErrorCode.INSUFFICIENT_POINT);
        }

        this.pointBalance -= point;
    }
}
