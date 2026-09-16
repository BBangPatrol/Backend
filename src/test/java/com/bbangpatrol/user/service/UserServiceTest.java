package com.bbangpatrol.user.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import com.bbangpatrol.point.repository.PointRepository;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.entity.VisitDetail;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 방문 내역 조회는 소프트 삭제된 리뷰가 섞여 들어올 수 있다. 응답에 새어 나가지 않는지 확인한다.
 * visited_at 과 대표 이미지(signature image)는 항상 존재한다는 전제로 동작한다.
 * 빵집이 없는 방문을 거르는 일과 정렬은 findHistoryByUserId 쿼리가 맡으므로 여기서는 보지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 6L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PointRepository pointRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewLikeRepository reviewLikeRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private VisitDetailRepository visitDetailRepository;
    @Mock
    private MissionProgressRepository missionProgressRepository;
    @Mock
    private R2Service r2Service;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("방문 내역은 쿼리 한 번으로 가져온다")
    void loadsHistoryInASingleQuery() {
        givenHistory(visitDetail(LocalDate.of(2026, 9, 5), null));

        userService.getBakeryList(USER_ID);

        // Visit -> VisitDetail -> Review 를 하나씩 따라가면 방문 수만큼 쿼리가 늘어난다
        verify(visitDetailRepository).findHistoryByUserId(USER_ID);
    }

    @Test
    @DisplayName("리뷰가 붙은 방문은 리뷰 정보를 함께 내려준다")
    void includesReviewOfVisit() {
        givenHistory(visitDetail(LocalDate.of(2026, 9, 5), review(null)));

        UserResponseDTO.VisitedBakeryListDTO result = userService.getBakeryList(USER_ID);

        assertThat(result.getVisits().get(0))
                .extracting(UserResponseDTO.VisitBakeryDTO::getStoreId,
                        UserResponseDTO.VisitBakeryDTO::getVisitDate,
                        UserResponseDTO.VisitBakeryDTO::getReviewId,
                        UserResponseDTO.VisitBakeryDTO::getRating)
                .containsExactly(101L, "2026-09-05", 55L, 4);
    }

    @Test
    @DisplayName("삭제된 리뷰는 방문 내역에 새어 나가지 않는다")
    void hidesSoftDeletedReview() {
        givenHistory(visitDetail(LocalDate.of(2026, 9, 5), review(LocalDateTime.now())));

        UserResponseDTO.VisitedBakeryListDTO result = userService.getBakeryList(USER_ID);

        // 방문 자체는 남고 리뷰만 빠진다
        assertThat(result.getVisits()).hasSize(1);
        assertThat(result.getVisits().get(0))
                .extracting(UserResponseDTO.VisitBakeryDTO::getReviewId,
                        UserResponseDTO.VisitBakeryDTO::getRating,
                        UserResponseDTO.VisitBakeryDTO::getReviewContent)
                .containsOnlyNulls();
    }

    @Test
    @DisplayName("포인트 내역 페이지 번호가 음수면 500 이 아니라 400 이다")
    void rejectsNegativePointHistoryPage() {
        assertThatThrownBy(() -> userService.getPointHistory(USER_ID, -1))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);

        verify(pointRepository, never()).findPointHistory(anyLong(), any());
    }

    @Test
    @DisplayName("내 리뷰 페이지 번호가 음수면 500 이 아니라 400 이다")
    void rejectsNegativeMyReviewPage() {
        assertThatThrownBy(() -> userService.getMyReviews(USER_ID, -1))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);

        verify(reviewRepository, never()).findMyReviews(anyLong(), any());
    }

    private void givenHistory(VisitDetail... details) {
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(visitDetailRepository.findHistoryByUserId(USER_ID)).thenReturn(List.of(details));
    }

    private User user() {
        return User.builder().id(USER_ID).name("빵순이").build();
    }

    private Bakery bakery() {
        Bakery bakery = Bakery.builder().id(101L).name("성심당").build();
        // 대표 이미지는 항상 최소 1장 존재한다는 전제(V6 마이그레이션에서 전체 빵집에 백필됨)
        bakery.getSignatureImages().add(
                SignatureImage.builder().id(201L).imageUrl("bakeries/101/signature_menu.jpg").build());
        return bakery;
    }

    private Review review(LocalDateTime deletedAt) {
        return Review.builder()
                .id(55L)
                .rating(4)
                .content("소금빵이 맛있어요")
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .deletedAt(deletedAt)
                .user(user())
                .bakery(bakery())
                .build();
    }

    private VisitDetail visitDetail(LocalDate visitedAt, Review review) {
        Visit visit = Visit.builder().id(1L).count(1).user(user()).bakery(bakery()).build();
        return VisitDetail.builder()
                .id(9L)
                .totalAmount(12000)
                .visitedAt(visitedAt)
                .createdAt(LocalDateTime.now())
                .visit(visit)
                .review(review)
                .build();
    }
}
