package com.bbangpatrol.review.controller;

//
//import com.bbangpatrol.common.util.ApiResponse;
//import com.bbangpatrol.review.dto.ReviewListResponse;
//import com.bbangpatrol.review.dto.ReviewRequest;
//import com.bbangpatrol.review.dto.ReviewResponse;
//import com.bbangpatrol.review.service.ReviewService;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;

//@RestController
//@RequestMapping("/api/v1/stores/{storeId}/reviews")
//public class ReviewController {
//
//    private final ReviewService reviewService;
//
//    public ReviewController(ReviewService reviewService) {
//        this.reviewService = reviewService;
//    }

//    @GetMapping
//    public ApiResponse<ReviewListResponse> getReviews(
//            @PathVariable Long storeId,
//            @RequestParam(required = false) Long cursor
//    ) {
//        return ApiResponse.onSuccess(reviewService.getReviews(storeId, cursor));
//    }
//
//    @PostMapping
//    public ApiResponse<ReviewResponse> postReview(
//            @PathVariable Long storeId,
//            @RequestBody ReviewRequest request,
//            //@RequestParam Long userId,
//            @AuthenticationPrincipal Long userId
//    ) {
//        return ApiResponse.onSuccess(reviewService.createReview(storeId, userId, request));
//    }
//
//    @PatchMapping("/{reviewId}")
//    public ApiResponse<ReviewResponse> editReview(
//            @PathVariable Long storeId,
//            @PathVariable Long reviewId,
//            @RequestBody ReviewRequest request,
//            //@RequestParam Long userId
//            @AuthenticationPrincipal Long userId
//    ) {
//        return ApiResponse.onSuccess(reviewService.editReview(reviewId, userId, request));
//    }
//
//    @DeleteMapping("/{reviewId}")
//    public ApiResponse deleteReview(
//            @PathVariable Long reviewId,
////            @AuthenticationPrincipal Long userId,
//            @RequestParam Long userId
//    ) {
//        reviewService.deleteReview(reviewId, userId);
//        return ApiResponse.onSuccess(null);
//    }
//
//    @PostMapping("/{reviewId}/like")
//    public ApiResponse toggleLike(
//            @PathVariable Long reviewId,
////            @AuthenticationPrincipal Long userId,
//            @RequestParam Long userId
//    ) {
//        reviewService.toggleLike(reviewId, userId);
//        return ApiResponse.onSuccess(null);
//    }
//}
