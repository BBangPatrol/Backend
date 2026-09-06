package com.bbangpatrol.review.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.dto.PageInfo;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final int SIZE = 20;

    private final UserRepository userRepository;
    private final BakeryRepository bakeryRepository;
    private final ReviewRepository reviewRepository;
    private final KeywordRepository keywordRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewKeywordRepository reviewKeywordRepository;
    private final R2Service r2Service;
    private final ReviewImageRepository reviewImageRepository;
    private final MissionEvaluator missionEvaluator;


    @Transactional
    public Review createReview(long userId, long storeId, ReviewCreatedRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(()-> new ApiException(ErrorCode.USER_NOT_FOUND));

        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(()-> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        Review review = reviewRepository.save(Review.builder()
                .rating(request.rating() != null ? request.rating() : Integer.valueOf(0))
                .content(request.content())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user)
                .bakery(bakery)
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

        return review;
    }

    public ReviewListResponse getReview(long storeId, Long cursor) {
        // 1개를 더 가져와서 hasNext를 판별
        List<Review> reviews = reviewRepository
                .findAllByBakeryWithCursor(storeId, cursor, PageRequest.of(0, SIZE + 1));

        boolean hasNext = reviews.size() > SIZE;
        List<Review> content = hasNext ? reviews.subList(0, SIZE) : reviews;
        
        // 리뷰 아이디만 따로 모으기 => 리뷰 이미지 조회, 리뷰 키워드 조회
        List<Long> reviewIds = content.stream().map(Review::getId).toList();

        Map<Long, List<String>> imageMap = reviewImageRepository.findAllByReviewIdIn(reviewIds).stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getId(),
                        Collectors.mapping(
                                // 저장된 값은 R2 키다. 응답에는 public URL 로 변환해서 내려준다
                                img -> r2Service.getPublicUrl(img.getImageUrl()),
                                Collectors.toList()
                        )
                ));

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
                    review.getLikeCount(),
                    review.getCreatedAt()
                )).toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
        PageInfo pageInfo = new PageInfo(content.size(), hasNext, nextCursor);

        return new ReviewListResponse(reviewResponses, pageInfo);
    }

    @Transactional
    public Review updateReview(long userId, long reviewId, ReviewUpdatedRequest request) {
        Review originalReview = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ApiException(ErrorCode.REVIEW_NOT_FOUND));
        
        // 작성자 아이디와 요청한 아이디가 다른 경우
        if (userId != originalReview.getUser().getId()) throw new ApiException(ErrorCode.USER_UNAUTHORIZE);

        Review newReview = reviewRepository.save(Review.builder()
                .id(originalReview.getId())
                .rating(request.rating() != null ? request.rating() : originalReview.getRating())
                .content(request.content() != null ? request.content() : originalReview.getContent())
                .likeCount(originalReview.getLikeCount())
                .createdAt(originalReview.getCreatedAt())
                .user(originalReview.getUser())
                .bakery(originalReview.getBakery())
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
        return newReview;
    }

    @Transactional
    public void deleteReview(long userId, long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ApiException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getUser().getId() != userId) throw new ApiException(ErrorCode.USER_UNAUTHORIZE);

        review.softDelete();
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

    private void addKeyword(Review review, List<Keyword> keywords) {
        keywords.forEach(k -> reviewKeywordRepository.save(
                ReviewKeyword.builder().review(review).keyword(k).build()));
    }

    private void deleteKeyword(List<Long> deleteList, List<ReviewKeyword> keywords) {
        keywords.forEach(k-> {
            if (deleteList.contains(k.getKeyword().getId())) reviewKeywordRepository.delete(k);
        });
    }

    private void uploadAndSaveImages(Review review, List<MultipartFile> images) {
        List<String> uploadedUrls = new ArrayList<>();
        try {
                images.forEach(file -> {
                    String url = null;
                    try {
                        url = r2Service.uploadImage(file, "reviews/" + review.getId());
                    } catch (IOException e) {
                        throw new ApiException(ErrorCode.R2_IO_ERROR);
                    }
                    uploadedUrls.add(url);
                    reviewImageRepository.save(ReviewImage.builder()
                            .review(review)
                            .origin(file.getOriginalFilename())
                            .imageUrl(url)
                            .build());
                });
        }catch (Exception e) {
            // 오류 발생 시 업로드한 파일들 전부 삭제
            uploadedUrls.forEach(url -> {
                try {
                    r2Service.deleteFile(url);
                } catch (Exception ex) {
                    log.error("R2 롤백 삭제 실패: {}", url);
                }
            });
            throw new ApiException(ErrorCode.R2_IO_ERROR);
        }
    }

    private void deleteImages(Review review, List<Long> imageIds) {
        List<ReviewImage> images = reviewImageRepository.findAllById(imageIds).stream()
                .filter(img -> img.getReview().getId().equals(review.getId()))
                .toList();
        List<String> urlsToDelete = images.stream().map(ReviewImage::getImageUrl).toList();
        reviewImageRepository.deleteAll(images);

        // 트랜잭션이 커밋된 이후 R2 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                urlsToDelete.forEach(url -> {
                    try {
                        r2Service.deleteFile(url);
                    } catch (Exception e) {
                        log.error("R2 파일 삭제가 실패했습니다. reviewId={}", review.getId());
                    }
                });
            }
        });
    }
}
