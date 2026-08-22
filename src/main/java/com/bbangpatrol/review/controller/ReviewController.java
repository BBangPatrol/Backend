package com.bbangpatrol.review.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.review.dto.ReviewApiResponse;
import com.bbangpatrol.review.dto.ReviewLikeResponse;
import com.bbangpatrol.review.dto.ReviewListResponse;
import com.bbangpatrol.review.dto.ReviewCreatedRequest;
import com.bbangpatrol.review.dto.ReviewUpdatedRequest;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/stores/{storeId}/reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse> getReviews(
            @PathVariable long storeId,
            @RequestParam(required = false) Long cursor
    ) {
        ReviewListResponse reviews = reviewService.getReview(storeId, cursor);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.onSuccess(
                HttpStatus.OK, "요청이 성공적입니다.", reviews));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createReview(
//            @AuthenticationPrincipal Long userId,
            @RequestParam long userId,
            @PathVariable long storeId,
            @ModelAttribute ReviewCreatedRequest request
    ) {
        Review review = reviewService.createReview(userId, storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.onSuccess(
                HttpStatus.CREATED, "요청이 성공적입니다.", new ReviewApiResponse(review.getId())));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> updateReview(
            //            @AuthenticationPrincipal Long userId,
            @RequestParam long userId,
            @PathVariable long reviewId,
            @ModelAttribute ReviewUpdatedRequest request
    ) {
        Review review = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(
                HttpStatus.OK, "리뷰가 수정되었습니다.", new ReviewApiResponse(review.getId())));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(
            //            @AuthenticationPrincipal Long userId,
            @RequestParam long userId,
            @PathVariable long reviewId
    ) {
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.onSuccess(
                HttpStatus.NO_CONTENT, "리뷰 삭제 요청이 성공적입니다.", null));
    }

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<ApiResponse> toggleReviewLikes(
            //            @AuthenticationPrincipal Long userId,
            @RequestParam long userId,
            @PathVariable long reviewId
    ) {
        boolean liked = reviewService.toggleReviewLike(userId, reviewId);
        HttpStatus status = liked ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.onSuccess(
                status, "요청이 성공적입니다.", new ReviewLikeResponse(liked)));
    }
}
