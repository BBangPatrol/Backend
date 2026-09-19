package com.bbangpatrol.review.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.dto.OffsetPageInfo;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.service.UploadedImage;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.service.MissionEvaluator;
import com.bbangpatrol.review.dto.ReviewListResponse;
import com.bbangpatrol.review.dto.ReviewCreatedRequest;
import com.bbangpatrol.review.dto.ReviewResponse;
import com.bbangpatrol.review.dto.ReviewUpdatedRequest;
import com.bbangpatrol.review.entity.*;
import com.bbangpatrol.review.repository.*;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.entity.VisitDetail;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final int SIZE = 20;
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    // 프론트의 업로드 상한(AddImages)과 같은 값
    private static final int MAX_REVIEW_IMAGES = 5;

    private final UserRepository userRepository;
    private final BakeryRepository bakeryRepository;
    private final ReviewRepository reviewRepository;
    private final KeywordRepository keywordRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewKeywordRepository reviewKeywordRepository;
    private final R2Service r2Service;
    private final ReviewImageRepository reviewImageRepository;
    private final VisitDetailRepository visitDetailRepository;
    private final MissionEvaluator missionEvaluator;


    @Transactional
    public Review createReview(long userId, long storeId, ReviewCreatedRequest request) {
        validateRating(request.rating(), true);
        validateNewImageCount(request.reviewImages());

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(()-> new ApiException(ErrorCode.USER_NOT_FOUND));

        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(()-> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        // 리뷰는 영수증 인증한 방문 한 건당 하나. 아직 리뷰를 안 쓴 가장 최근 방문에 붙인다
        VisitDetail visitDetail = visitDetailRepository
                .findReviewable(userId, storeId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VISIT_NOT_VERIFIED));

        Review review = reviewRepository.save(Review.builder()
                .rating(request.rating())
                .content(request.content())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user)
                .bakery(bakery)
                .visitDetail(visitDetail)
                .build());
        
        // 키워드가 있는 경우만 처리
        if (request.keywordIds() != null && !request.keywordIds().isEmpty()) {
            addKeyword(review, keywordRepository.findAllById(request.keywordIds()));
        }

        // 사진이 있는 경우만 처리
        if (request.reviewImages() != null && !request.reviewImages().isEmpty()) {
            uploadAndSaveImages(review,  request.reviewImages());
        }

        // 리뷰 미션 진행도 갱신
        missionEvaluator.onReviewCreated(userId, bakery.getRegion());

        // 평점은 bakery.avg_rating 에 저장된 값을 내려주므로 리뷰가 늘면 그 값도 다시 채운다
        bakeryRepository.refreshAvgRating(bakery.getId());

        return review;
    }

    // userId 는 비로그인 조회면 null 이다
    public ReviewListResponse getReview(long storeId, int page, Long userId) {
        // PageRequest.of 가 음수에 IllegalArgumentException 을 던져 500 이 된다
        if (page < 0) throw new ApiException(ErrorCode.BAD_REQUEST);

        Page<Review> reviews = reviewRepository.findPageByBakeryId(storeId, PageRequest.of(page, SIZE));
        List<Review> content = reviews.getContent();

        // 리뷰 아이디만 따로 모으기 => 리뷰 이미지 조회, 리뷰 키워드 조회
        List<Long> reviewIds = content.stream().map(Review::getId).toList();

        List<ReviewImage> reviewImages = reviewImageRepository.findAllByReviewIdIn(reviewIds);

        Map<Long, List<String>> imageMap = reviewImages.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getId(),
                        Collectors.mapping(
                                // 저장된 값은 R2 키다. 응답에는 public URL 로 변환해서 내려준다
                                img -> r2Service.getPublicUrl(img.getImageUrl()),
                                Collectors.toList()
                        )
                ));

        // 목록에서는 원본 대신 썸네일만 렌더링하도록 함께 내려준다.
        // 썸네일이 없는 행(thumbnail_url IS NULL)은 원본으로 폴백해 깨진 이미지가 나가지 않게 한다
        Map<Long, List<String>> thumbnailMap = reviewImages.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getId(),
                        Collectors.mapping(
                                img -> r2Service.getPublicUrl(
                                        StringUtils.hasText(img.getThumbnailUrl())
                                                ? img.getThumbnailUrl()
                                                : img.getImageUrl()),
                                Collectors.toList()
                        )
                ));

        // 로그인한 사용자가 좋아요를 누른 리뷰만 한 번에 조회한다
        Set<Long> likedReviewIds = (userId == null || reviewIds.isEmpty())
                ? Set.of()
                : Set.copyOf(reviewLikeRepository.findLikedReviewIds(userId, reviewIds));

        Map<Long, List<Long>> keywordMap = reviewKeywordRepository.findAllByReviewIdIn(reviewIds).stream()
                .collect(Collectors.groupingBy(
                        k -> k.getReview().getId(),
                        Collectors.mapping(
                                k -> k.getKeyword().getId(),
                                Collectors.toList()
                        )
                ));

        List<ReviewResponse> reviewResponses = content.stream().map(
                review -> new ReviewResponse(
                    review.getId(),
                    review.getUser().getId(),
                    review.getUser().getName(),
                    r2Service.getPublicUrl(review.getUser().getUserImage()),
                    review.getRating(),
                    review.getContent(),
                    keywordMap.getOrDefault(review.getId(), List.of()),
                    imageMap.getOrDefault(review.getId(), List.of()),
                    thumbnailMap.getOrDefault(review.getId(), List.of()),
                    review.getLikeCount(),
                    likedReviewIds.contains(review.getId()),
                    review.getCreatedAt()
                )).toList();

        OffsetPageInfo pageInfo = new OffsetPageInfo(
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages(),
                reviews.hasNext());

        return new ReviewListResponse(reviewResponses, reviews.getTotalElements(), pageInfo);
    }

    @Transactional
    public Review updateReview(long userId, long reviewId, ReviewUpdatedRequest request) {
        validateRating(request.rating(), false);

        Review originalReview = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ApiException(ErrorCode.REVIEW_NOT_FOUND));

        // 아래 save 는 merge 라 deletedAt 이 null 로 덮인다. 막지 않으면 삭제한 리뷰가 되살아난다
        if (originalReview.getDeletedAt() != null) throw new ApiException(ErrorCode.REVIEW_NOT_FOUND);

        // 작성자 아이디와 요청한 아이디가 다른 경우
        if (userId != originalReview.getUser().getId()) throw new ApiException(ErrorCode.USER_UNAUTHORIZE);

        validateImageCountAfterUpdate(reviewId, request);

        Review newReview = reviewRepository.save(Review.builder()
                .id(originalReview.getId())
                .rating(request.rating() != null ? request.rating() : originalReview.getRating())
                .content(request.content() != null ? request.content() : originalReview.getContent())
                .likeCount(originalReview.getLikeCount())
                .createdAt(originalReview.getCreatedAt())
                .user(originalReview.getUser())
                .bakery(originalReview.getBakery())
                // merge 라 빠뜨리면 연결이 끊긴다
                .visitDetail(originalReview.getVisitDetail())
                .build());

        // 추가할 키워드가 있는 경우
        if (request.keywordIds() != null && !request.keywordIds().isEmpty()) {
            addKeyword(newReview, keywordRepository.findAllById(request.keywordIds()));
        }

        // 삭제할 키워드가 있는 경우
        if (request.deleteKeywordIds() != null && !request.deleteKeywordIds().isEmpty()) {
            deleteKeyword(request.deleteKeywordIds(), reviewKeywordRepository.findAllByReview(originalReview));
        }

        // 추가할 사진이 있는 경우만 처리
        if (request.reviewImages() != null && !request.reviewImages().isEmpty()) {
            uploadAndSaveImages(newReview, request.reviewImages());
        }

        // 삭제할 사진이 있는 경우만 처리
        if (request.deleteImages() != null && !request.deleteImages().isEmpty()) {
            deleteImages(newReview, request.deleteImages());
        }

        // 별점이 바뀌었을 수 있으니 가게 평점을 다시 채운다
        bakeryRepository.refreshAvgRating(newReview.getBakery().getId());

        return newReview;
    }

    @Transactional
    public void deleteReview(long userId, long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ApiException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getUser().getId() != userId) throw new ApiException(ErrorCode.USER_UNAUTHORIZE);

        review.softDelete();

        // 지운 별점이 평균에서 빠지도록 다시 채운다. 마지막 리뷰였다면 NULL 이 된다
        bakeryRepository.refreshAvgRating(review.getBakery().getId());
    }

    @Transactional
    public boolean toggleReviewLike(long userId, long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ApiException(ErrorCode.USER_NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ApiException(ErrorCode.REVIEW_NOT_FOUND));

        ReviewLike existing = reviewLikeRepository.findByUserAndReview(user, review);

        if (existing != null) {
            reviewLikeRepository.delete(existing);
            review.decreaseLikeCount();
            return false;
        }

        reviewLikeRepository.save(ReviewLike.builder()
                .user(user)
                .review(review)
                .createdAt(LocalDateTime.now())
                .build());
        review.increaseLikeCount();
        return true;
    }

    /**
     * 별점은 1~5 만 받는다. 컨트롤러가 @ModelAttribute 로 받아 빈 검증을 걸기 어려워
     * BakeryService.validateSearchRequest 처럼 서비스에서 막는다.
     *
     * 막지 않으면 두 가지가 깨진다. 별점을 빼고 보내면 0 점으로 저장돼 가게 평점이
     * 깎이고, 10 이상을 보내면 평균이 bakery.avg_rating(DECIMAL(2,1)) 범위를 넘어
     * refreshAvgRating 의 UPDATE 가 실패하면서 리뷰 작성 자체가 500 으로 롤백된다.
     *
     * 수정은 별점을 안 보내면 기존 값을 유지하는 규칙이라 null 을 허용한다.
     */
    private void validateRating(Integer rating, boolean required) {
        if (rating == null) {
            if (required) throw new ApiException(ErrorCode.INVALID_REVIEW_RATING);
            return;
        }

        if (rating < MIN_RATING || rating > MAX_RATING) throw new ApiException(ErrorCode.INVALID_REVIEW_RATING);
    }

    /**
     * 리뷰 사진은 최대 MAX_REVIEW_IMAGES 장. 요청 용량 한도(장당 15MB / 요청 60MB) 안에서는
     * 수십 장도 통과해 R2 비용과 목록 응답 크기가 늘어난다. 업로드 전에 막아야
     * R2 에 올렸다가 되돌리는 일이 없다.
     */
    private void validateNewImageCount(List<MultipartFile> images) {
        if (images != null && images.size() > MAX_REVIEW_IMAGES) {
            throw new ApiException(ErrorCode.TOO_MANY_REVIEW_IMAGES);
        }
    }

    // 수정은 지우기로 한 사진을 뺀 뒤의 장수로 본다. 사진을 안 올리는 요청은 쿼리도 하지 않는다
    private void validateImageCountAfterUpdate(long reviewId, ReviewUpdatedRequest request) {
        int adding = request.reviewImages() == null ? 0 : request.reviewImages().size();
        if (adding == 0) return;

        List<Long> deleting = request.deleteImages() == null ? List.of() : request.deleteImages();
        long remaining = reviewImageRepository.findAllByReviewIdIn(List.of(reviewId)).stream()
                .filter(image -> !deleting.contains(image.getId()))
                .count();

        if (remaining + adding > MAX_REVIEW_IMAGES) throw new ApiException(ErrorCode.TOO_MANY_REVIEW_IMAGES);
    }

    private void addKeyword(Review review, List<Keyword> keywords) {
        // 이미 달린 키워드를 또 저장하면 조회 응답의 keywords 에 같은 id 가 여러 번 실린다.
        // 요청 안에 같은 키워드가 두 번 들어온 경우도 add 가 false 를 돌려주며 걸러진다
        Set<Long> attached = reviewKeywordRepository.findAllByReview(review).stream()
                .map(reviewKeyword -> reviewKeyword.getKeyword().getId())
                .collect(Collectors.toCollection(HashSet::new));

        keywords.stream()
                .filter(keyword -> attached.add(keyword.getId()))
                .forEach(keyword -> reviewKeywordRepository.save(
                        ReviewKeyword.builder().review(review).keyword(keyword).build()));
    }

    private void deleteKeyword(List<Long> deleteList, List<ReviewKeyword> keywords) {
        keywords.forEach(k-> {
            if (deleteList.contains(k.getKeyword().getId())) reviewKeywordRepository.delete(k);
        });
    }

    private void uploadAndSaveImages(Review review, List<MultipartFile> images) {
        List<UploadedImage> uploaded = new ArrayList<>();
        try {
                images.forEach(file -> {
                    UploadedImage image;
                    try {
                        image = r2Service.uploadImageWithThumbnail(file, "reviews/" + review.getId());
                    } catch (IOException e) {
                        throw new ApiException(ErrorCode.R2_IO_ERROR);
                    }
                    uploaded.add(image);
                    reviewImageRepository.save(ReviewImage.builder()
                            .review(review)
                            .origin(file.getOriginalFilename())
                            .imageUrl(image.key())
                            .thumbnailUrl(image.thumbnailKey())
                            .build());
                });
        }catch (Exception e) {
            // 오류 발생 시 업로드한 파일들 전부 삭제
            uploaded.forEach(image -> {
                try {
                    r2Service.deleteImage(image.key(), image.thumbnailKey());
                } catch (Exception ex) {
                    log.error("R2 롤백 삭제 실패: {}", image.key());
                }
            });
            throw new ApiException(ErrorCode.R2_IO_ERROR);
        }
    }

    private void deleteImages(Review review, List<Long> imageIds) {
        List<ReviewImage> images = reviewImageRepository.findAllById(imageIds).stream()
                .filter(img -> img.getReview().getId().equals(review.getId()))
                .toList();
        // 엔티티가 지워진 뒤에도 쓸 수 있도록 key 를 미리 복사해둔다
        List<UploadedImage> keysToDelete = images.stream()
                .map(img -> new UploadedImage(img.getImageUrl(), img.getThumbnailUrl()))
                .toList();
        reviewImageRepository.deleteAll(images);

        // 트랜잭션이 커밋된 이후 R2 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keysToDelete.forEach(image -> {
                    try {
                        r2Service.deleteImage(image.key(), image.thumbnailKey());
                    } catch (Exception e) {
                        log.error("R2 파일 삭제가 실패했습니다. reviewId={}", review.getId());
                    }
                });
            }
        });
    }
}
