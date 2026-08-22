package com.bbangpatrol.review.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.review.dto.ReviewCreatedResponse;
import com.bbangpatrol.review.dto.ReviewListResponse;
import com.bbangpatrol.review.dto.ReviewRequest;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @ModelAttribute ReviewRequest request
    ) {
        Review review = reviewService.createReview(userId, storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.onSuccess(
                HttpStatus.CREATED, "요청이 성공적입니다.", new ReviewCreatedResponse(review.getId())));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> updateReview() {
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview() {
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<ApiResponse> toggleReviewLikes() {
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }
}
