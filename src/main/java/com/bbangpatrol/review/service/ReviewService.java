package com.bbangpatrol.review.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.review.dto.ReviewListResponse;
import com.bbangpatrol.review.dto.ReviewRequest;
import com.bbangpatrol.review.dto.ReviewResponse;
import com.bbangpatrol.review.entity.Keyword;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewImage;
import com.bbangpatrol.review.entity.ReviewKeyword;
import com.bbangpatrol.review.repository.*;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


    @Transactional
    public Review createReview(long userId, long storeId, ReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ApiException(ErrorCode.USER_NOT_FOUND));

        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(()-> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        Review review = reviewRepository.save(Review.builder()
                .rating(request.rating())
                .content(request.content())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .user(user)
                .bakery(bakery)
                .build());
        
        // 키워드가 있는 경우만 처리
        if (request.keywordIds() != null && !request.keywordIds().isEmpty()) {
            List<Keyword> keywords = keywordRepository.findAllById(request.keywordIds());
            keywords.forEach(k -> reviewKeywordRepository.save(
                    ReviewKeyword.builder().review(review).keyword(k).build()));
        }

        // 사진이 있는 경우만 처리
        if (request.reviewImages() != null && !request.reviewImages().isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            try {
                request.reviewImages().forEach(file -> {
                    String url = null;
                    try {
                        url = r2Service.uploadImage(file, "reviews/" + review.getId());
                    } catch (IOException e) {
                        throw new ApiException(ErrorCode.R2_UPLOAD_IO_ERROR);
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
                    throw e;
                });

                throw new ApiException(ErrorCode.R2_UPLOAD_IO_ERROR);
            }
        }
        return review;
    }

    public ReviewListResponse getReview(long storeId, long cursor) {
        // 1개를 더 가져와서 hasNext를 판별
        List<Review> reviews = reviewRepository
                .findAllByBakeryWithCursor(storeId, cursor, PageRequest.of(0, SIZE + 1));

        boolean hasNext = reviews.size() > SIZE;
        List<Review> content = hasNext ? reviews.subList(0, SIZE) : reviews;
        
        // 리뷰 아이디만 따로 모으기 => 리뷰 이미지 조회, 리뷰 키워드 조회
        List<Long> reviewIds = content.stream().map(Review::getId).toList();

        List<ReviewImage> reviewImages = reviewImageRepository.findAllByReviewIdIn(reviewIds);
        Map<Long, List<String>> imageMap = reviewImageRepository.findAllByReviewIdIn(reviewIds).stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getId(),
                        Collectors.mapping(
                                ReviewImage::getImageUrl,
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
//        List<ReviewResponse> reviewResponses =
        return new ReviewListResponse(null, null);
    }
}
