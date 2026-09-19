package com.bbangpatrol.review.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.service.MissionEvaluator;
import com.bbangpatrol.review.dto.ReviewCreatedRequest;
import com.bbangpatrol.review.dto.ReviewListResponse;
import com.bbangpatrol.review.dto.ReviewResponse;
import com.bbangpatrol.review.dto.ReviewUpdatedRequest;
import com.bbangpatrol.review.entity.Keyword;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewKeyword;
import com.bbangpatrol.review.repository.KeywordRepository;
import com.bbangpatrol.review.repository.ReviewImageRepository;
import com.bbangpatrol.review.repository.ReviewKeywordRepository;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.entity.VisitDetail;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리뷰는 영수증 인증한 방문 한 건에 하나씩만 달린다. 그 규칙과, 가게 리뷰 목록의 오프셋 페이징을 검증한다.
 * findReviewable 쿼리 자체(소프트 삭제된 리뷰를 비운 것으로 볼지 등)는 DB 가 있어야 확인할 수 있어
 * 여기서는 서비스가 그 쿼리에 무엇을 넘기고 결과를 어떻게 쓰는지까지만 본다.
 * 가게 평점 재계산도 같은 이유로(평균은 UPDATE 문 안에서 계산된다) 호출 여부까지만 본다.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final long USER_ID = 7L;
    private static final long STORE_ID = 3L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private ReviewLikeRepository reviewLikeRepository;
    @Mock
    private ReviewKeywordRepository reviewKeywordRepository;
    @Mock
    private R2Service r2Service;
    @Mock
    private ReviewImageRepository reviewImageRepository;
    @Mock
    private VisitDetailRepository visitDetailRepository;
    @Mock
    private MissionEvaluator missionEvaluator;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("영수증 인증한 방문이 없으면 리뷰를 쓸 수 없다")
    void rejectsReviewWithoutVerifiedVisit() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(bakeryRepository.findById(STORE_ID)).thenReturn(Optional.of(bakery()));
        when(visitDetailRepository.findReviewable(eq(USER_ID), eq(STORE_ID), any(Pageable.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> reviewService.createReview(USER_ID, STORE_ID, request()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_NOT_VERIFIED);

        // 방문 기록이 없으면 리뷰 행 자체가 만들어지면 안 된다
        verify(reviewRepository, never()).save(any());
        verify(missionEvaluator, never()).onReviewCreated(anyLong(), any());
        verify(bakeryRepository, never()).refreshAvgRating(anyLong());
    }

    @Test
    @DisplayName("리뷰를 쓰면 아직 리뷰가 없는 방문 기록에 연결된다")
    void linksReviewToReviewableVisitDetail() {
        VisitDetail visitDetail = visitDetail();

        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(bakeryRepository.findById(STORE_ID)).thenReturn(Optional.of(bakery()));
        when(visitDetailRepository.findReviewable(eq(USER_ID), eq(STORE_ID), any(Pageable.class)))
                .thenReturn(List.of(visitDetail));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createReview(USER_ID, STORE_ID, request());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getVisitDetail()).isSameAs(visitDetail);
    }

    @Test
    @DisplayName("방문 기록은 가장 최근 한 건만 조회한다")
    void looksUpOnlyTheLatestReviewableVisit() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(bakeryRepository.findById(STORE_ID)).thenReturn(Optional.of(bakery()));
        when(visitDetailRepository.findReviewable(eq(USER_ID), eq(STORE_ID), any(Pageable.class)))
                .thenReturn(List.of(visitDetail()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createReview(USER_ID, STORE_ID, request());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(visitDetailRepository).findReviewable(eq(USER_ID), eq(STORE_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("리뷰를 삭제하면 방문 기록 연결이 풀려 같은 방문에 다시 쓸 수 있다")
    void softDeleteReleasesVisitDetail() {
        Review review = Review.builder()
                .id(11L)
                .rating(5)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user())
                .bakery(bakery())
                .visitDetail(visitDetail())
                .build();
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(USER_ID, 11L);

        assertThat(review.getDeletedAt()).isNotNull();
        // UNIQUE 제약이 걸린 컬럼이라 비워 두지 않으면 그 방문에는 영영 다시 못 쓴다
        assertThat(review.getVisitDetail()).isNull();
    }

    @Test
    @DisplayName("삭제된 리뷰는 수정으로 되살릴 수 없다")
    void cannotResurrectDeletedReviewByUpdating() {
        Review deleted = Review.builder()
                .id(11L)
                .rating(5)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now().minusDays(1))
                .user(user())
                .bakery(bakery())
                .build();
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> reviewService.updateReview(
                USER_ID, 11L, new ReviewUpdatedRequest(3, "되살리기", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);

        // merge 로 deletedAt 이 null 이 되면 인증 한 번에 살아 있는 리뷰가 둘이 될 수 있다
        verify(reviewRepository, never()).save(any());
        verify(bakeryRepository, never()).refreshAvgRating(anyLong());
    }

    @Test
    @DisplayName("리뷰를 쓰면 가게 평점을 다시 계산한다")
    void refreshesAvgRatingOnCreate() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(bakeryRepository.findById(STORE_ID)).thenReturn(Optional.of(bakery()));
        when(visitDetailRepository.findReviewable(eq(USER_ID), eq(STORE_ID), any(Pageable.class)))
                .thenReturn(List.of(visitDetail()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createReview(USER_ID, STORE_ID, request());

        // 평점은 bakery.avg_rating 에 저장된 값을 그대로 내려주므로 여기서 다시 채워야 한다
        verify(bakeryRepository).refreshAvgRating(STORE_ID);
    }

    @Test
    @DisplayName("리뷰를 수정하면 가게 평점을 다시 계산한다")
    void refreshesAvgRatingOnUpdate() {
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(reviewOf(11L)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.updateReview(USER_ID, 11L, new ReviewUpdatedRequest(2, "별점만 내렸다", null, null, null, null));

        verify(bakeryRepository).refreshAvgRating(STORE_ID);
    }

    @Test
    @DisplayName("리뷰를 삭제하면 가게 평점을 다시 계산한다")
    void refreshesAvgRatingOnDelete() {
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(reviewOf(11L)));

        reviewService.deleteReview(USER_ID, 11L);

        // 마지막 리뷰였다면 이 호출로 avg_rating 이 NULL(별점 없음)이 된다
        verify(bakeryRepository).refreshAvgRating(STORE_ID);
    }

    @Test
    @DisplayName("별점이 1~5 밖이거나 비어 있으면 리뷰를 쓸 수 없다")
    void rejectsRatingOutOfRange() {
        // 0 점이면 가게 평점이 깎이고, 10 이상이면 avg_rating(DECIMAL(2,1)) 범위를 넘겨 작성 자체가 터진다
        for (Integer rating : new Integer[] {null, 0, -1, 6, 100}) {
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, STORE_ID,
                    new ReviewCreatedRequest(rating, "소금빵이 진짜 맛있어요", null, null)))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REVIEW_RATING);
        }

        // 별점을 보기도 전에 막아야 방문 조회·저장까지 가지 않는다
        verify(userRepository, never()).findByIdForUpdate(anyLong());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 수정은 별점을 빼면 기존 값을 유지하고, 범위를 벗어나면 막는다")
    void rejectsRatingOutOfRangeOnUpdate() {
        assertThatThrownBy(() -> reviewService.updateReview(USER_ID, 11L,
                new ReviewUpdatedRequest(0, "별점만 0 으로", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REVIEW_RATING);
        verify(reviewRepository, never()).findById(anyLong());

        when(reviewRepository.findById(11L)).thenReturn(Optional.of(reviewOf(11L)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review updated = reviewService.updateReview(USER_ID, 11L,
                new ReviewUpdatedRequest(null, "내용만 고쳤다", null, null, null, null));

        assertThat(updated.getRating()).isEqualTo(4);
        assertThat(updated.getContent()).isEqualTo("내용만 고쳤다");
    }

    @Test
    @DisplayName("이미 달린 키워드는 다시 저장하지 않는다")
    void doesNotAttachKeywordTwice() {
        Review review = reviewOf(11L);
        Keyword attached = Keyword.builder().id(1L).label("소금빵").build();
        Keyword added = Keyword.builder().id(2L).label("친절해요").build();

        when(reviewRepository.findById(11L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keywordRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(attached, added));
        // updateReview 는 merge 로 새 인스턴스를 만들어 넘기므로 any() 로 받는다
        when(reviewKeywordRepository.findAllByReview(any(Review.class))).thenReturn(List.of(
                ReviewKeyword.builder().id(5L).review(review).keyword(attached).build()));

        reviewService.updateReview(USER_ID, 11L,
                new ReviewUpdatedRequest(4, "키워드만 추가", null, List.of(1L, 2L), null, null));

        // 중복 저장하면 리뷰 조회 응답의 keywords 에 같은 id 가 두 번 실린다
        ArgumentCaptor<ReviewKeyword> captor = ArgumentCaptor.forClass(ReviewKeyword.class);
        verify(reviewKeywordRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyword().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("페이지 번호가 음수면 500 이 아니라 400 이다")
    void rejectsNegativePage() {
        assertThatThrownBy(() -> reviewService.getReview(STORE_ID, -1, USER_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);

        verify(reviewRepository, never()).findPageByBakeryId(anyLong(), any());
    }

    @Test
    @DisplayName("가게 리뷰 목록은 요청한 페이지와 전체 개수를 함께 내려준다")
    void returnsOffsetPageInfo() {
        List<Review> content = LongStream.rangeClosed(1, 20).mapToObj(this::reviewOf).toList();
        Page<Review> page = new PageImpl<>(content, PageRequest.of(1, 20), 45);
        when(reviewRepository.findPageByBakeryId(eq(STORE_ID), any(Pageable.class))).thenReturn(page);
        when(reviewImageRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(reviewKeywordRepository.findAllByReviewIdIn(any())).thenReturn(List.of());

        ReviewListResponse response = reviewService.getReview(STORE_ID, 1, null);

        assertThat(response.reviews()).hasSize(20);
        assertThat(response.count()).isEqualTo(45);
        assertThat(response.pageInfo().page()).isEqualTo(1);
        assertThat(response.pageInfo().size()).isEqualTo(20);
        assertThat(response.pageInfo().totalElements()).isEqualTo(45);
        assertThat(response.pageInfo().totalPages()).isEqualTo(3);
        assertThat(response.pageInfo().hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext 가 false 다")
    void lastPageHasNoNext() {
        Page<Review> page = new PageImpl<>(List.of(reviewOf(1L)), PageRequest.of(2, 20), 41);
        when(reviewRepository.findPageByBakeryId(eq(STORE_ID), any(Pageable.class))).thenReturn(page);
        when(reviewImageRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(reviewKeywordRepository.findAllByReviewIdIn(any())).thenReturn(List.of());

        ReviewListResponse response = reviewService.getReview(STORE_ID, 2, null);

        assertThat(response.pageInfo().hasNext()).isFalse();
        assertThat(response.pageInfo().totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("로그인한 사용자가 좋아요를 누른 리뷰는 isLike 가 true 다")
    void marksReviewsLikedByViewer() {
        Page<Review> page = new PageImpl<>(List.of(reviewOf(1L), reviewOf(2L)), PageRequest.of(0, 20), 2);
        when(reviewRepository.findPageByBakeryId(eq(STORE_ID), any(Pageable.class))).thenReturn(page);
        when(reviewImageRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(reviewKeywordRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(reviewLikeRepository.findLikedReviewIds(eq(USER_ID), any())).thenReturn(List.of(2L));

        ReviewListResponse response = reviewService.getReview(STORE_ID, 0, USER_ID);

        assertThat(response.reviews()).extracting(ReviewResponse::id, ReviewResponse::isLike)
                .containsExactly(tuple(1L, false), tuple(2L, true));
    }

    @Test
    @DisplayName("비로그인 조회는 좋아요를 조회하지 않고 전부 false 로 내려준다")
    void anonymousViewerSeesNoLikes() {
        Page<Review> page = new PageImpl<>(List.of(reviewOf(1L)), PageRequest.of(0, 20), 1);
        when(reviewRepository.findPageByBakeryId(eq(STORE_ID), any(Pageable.class))).thenReturn(page);
        when(reviewImageRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(reviewKeywordRepository.findAllByReviewIdIn(any())).thenReturn(List.of());

        ReviewListResponse response = reviewService.getReview(STORE_ID, 0, null);

        assertThat(response.reviews()).extracting(ReviewResponse::isLike).containsExactly(false);
        verify(reviewLikeRepository, never()).findLikedReviewIds(anyLong(), any());
    }

    private ReviewCreatedRequest request() {
        return new ReviewCreatedRequest(5, "소금빵이 진짜 맛있어요", null, null);
    }

    private User user() {
        return User.builder().id(USER_ID).name("빵순이").build();
    }

    private Bakery bakery() {
        return Bakery.builder().id(STORE_ID).name("성심당").build();
    }

    private VisitDetail visitDetail() {
        return VisitDetail.builder()
                .id(99L)
                .totalAmount(12000)
                .visitedAt(LocalDate.of(2026, 9, 5))
                .createdAt(LocalDateTime.now())
                .visit(Visit.create(user(), bakery()))
                .build();
    }

    private Review reviewOf(long id) {
        return Review.builder()
                .id(id)
                .rating(4)
                .content("맛있어요")
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user())
                .bakery(bakery())
                .build();
    }
}
