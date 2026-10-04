package com.bbangpatrol.user.service;

import com.bbangpatrol.auth.service.AuthService;
import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 탈퇴는 되돌릴 수 없고, 빠뜨린 정리는 다른 이용자 화면에 그대로 남는다.
 *  - 리뷰를 내리지 않으면 탈퇴한 사람의 닉네임이 계속 보인다.
 *  - 평점을 다시 계산하지 않으면 사라진 리뷰의 별점이 빵집 평점에 남는다.
 *  - 좋아요를 회수하지 않으면 남의 리뷰 like_count 가 실제보다 크게 남는다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawServiceTest {

    private static final long USER_ID = 6L;
    private static final String ACCESS_TOKEN = "access-token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewLikeRepository reviewLikeRepository;
    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AccountWithdrawService accountWithdrawService;

    @Test
    @DisplayName("탈퇴하면 리뷰가 내려가고 그 빵집 평점이 다시 계산된다")
    void softDeletesReviewsAndRefreshesRating() {
        User user = user();
        Bakery bakeryA = bakery(11L);
        Bakery bakeryB = bakery(22L);
        Review r1 = review(user, bakeryA);
        Review r2 = review(user, bakeryA); // 같은 빵집 두 건이어도 평점 계산은 한 번
        Review r3 = review(user, bakeryB);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reviewRepository.findAllByUser_IdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(r1, r2, r3));
        when(reviewLikeRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        accountWithdrawService.withdraw(USER_ID, ACCESS_TOKEN);

        assertThat(r1.getDeletedAt()).isNotNull();
        assertThat(r2.getDeletedAt()).isNotNull();
        assertThat(r3.getDeletedAt()).isNotNull();
        verify(bakeryRepository).refreshAvgRating(11L);
        verify(bakeryRepository).refreshAvgRating(22L);
    }

    @Test
    @DisplayName("내가 누른 좋아요를 회수하고 남의 리뷰 좋아요 수를 낮춘다")
    void takesBackLikes() {
        User user = user();
        User other = user();
        Review liveReview = review(other, bakery(11L));
        liveReview.increaseLikeCount();
        Review deletedReview = review(other, bakery(12L));
        deletedReview.increaseLikeCount();
        deletedReview.softDelete();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reviewRepository.findAllByUser_IdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of());
        when(reviewLikeRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(like(user, liveReview), like(user, deletedReview)));

        accountWithdrawService.withdraw(USER_ID, ACCESS_TOKEN);

        assertThat(liveReview.getLikeCount()).isZero();
        // 이미 지워진 리뷰의 수치는 건드리지 않는다
        assertThat(deletedReview.getLikeCount()).isEqualTo(1);
        verify(reviewLikeRepository).deleteAll(any());
    }

    @Test
    @DisplayName("탈퇴 표시와 토큰 무효화가 함께 이뤄진다")
    void marksWithdrawnAndLogsOut() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reviewRepository.findAllByUser_IdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of());
        when(reviewLikeRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        accountWithdrawService.withdraw(USER_ID, ACCESS_TOKEN);

        assertThat(user.getDeletedAt()).isNotNull();
        // 남겨두면 탈퇴 후에도 토큰 재발급으로 계정이 되살아난다
        assertThat(user.getRefreshToken()).isNull();
        verify(authService).logout(USER_ID, ACCESS_TOKEN);
    }

    @Test
    @DisplayName("이미 탈퇴한 계정은 다시 탈퇴되지 않는다")
    void rejectsAlreadyWithdrawn() {
        User user = user();
        user.withdraw();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // 막지 않으면 요청할 때마다 보관 기간이 뒤로 밀려 영원히 파기되지 않는다
        assertThatThrownBy(() -> accountWithdrawService.withdraw(USER_ID, ACCESS_TOKEN))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(authService, never()).logout(anyLong(), any());
        verify(reviewRepository, never()).findAllByUser_IdAndDeletedAtIsNull(anyLong());
    }

    private User user() {
        return User.ofKakao(1234L, "test@example.com", "테스트");
    }

    private Bakery bakery(Long id) {
        return Bakery.builder().id(id).name("빵집" + id).build();
    }

    private Review review(User user, Bakery bakery) {
        return Review.builder()
                .rating(5)
                .content("맛있어요")
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user)
                .bakery(bakery)
                .build();
    }

    private ReviewLike like(User user, Review review) {
        return ReviewLike.builder().user(user).review(review).build();
    }
}
