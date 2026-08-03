//package com.bbangpatrol.review.service;
//
//import com.bbangpatrol.auth.repository.UserRepository;
//import com.bbangpatrol.common.dto.PageInfo;
//import com.bbangpatrol.review.dto.ReviewListResponse;
//import com.bbangpatrol.review.dto.ReviewRequest;
//import com.bbangpatrol.review.dto.ReviewResponse;
//import com.bbangpatrol.review.entity.Review;
//import com.bbangpatrol.review.exception.ReviewAccessDeniedException;
//import com.bbangpatrol.review.exception.ReviewNotFoundException;
//import com.bbangpatrol.review.repository.ReviewRepository;
//import com.bbangpatrol.user.entity.User;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//public class ReviewService {
//
//    private final ReviewRepository reviewRepository;
////    private final BakeryRepository bakeryRepository;
////    private final UserRepository userRepository;
//    private final int PAGE_SIZE = 20;
//
//
//    public ReviewService(ReviewRepository reviewRepository
////                         BakeryRepository bakeryRepository,
////                         UserRepository userRepository
//    ) {
//        this.reviewRepository = reviewRepository;
////        this.bakeryRepository = bakeryRepository;
////        this.userRepository = userRepository;
//    }
//
//
//    public ReviewListResponse getReviews(Long storeId, Long cursor) {
//
//        List<Review> reviews = reviewRepository.findByStoreIdWithCursor(
//                storeId, cursor, PageRequest.ofSize(PAGE_SIZE + 1));
//
//        boolean hasNext = reviews.size() > PAGE_SIZE;
//        List<Review> content = hasNext ? reviews.subList(0, PAGE_SIZE) : reviews;
//        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
//
//        List<ReviewResponse> reviewResponses = content.stream()
//                .map(ReviewResponse::from)
//                .toList();
//
//        return new ReviewListResponse(reviewResponses, new PageInfo(PAGE_SIZE, hasNext, nextCursor));
//    }
//
////    @Transactional
////    public ReviewResponse createReview(Long storeId, Long userId, ReviewRequest request) {
////        User user = userRepository.findById(userId)
////                .orElseThrow(() -> new UserNotFoundException(userId));
////        Bakery bakery = bakeryRepository.findById(storeId)
////                .orElseThrow(() -> new BakeryNotFoundException(storeId));
////
////        Review review = Review.builder()
////                .user(1)
////                .bakery(1)
////                .rating(request.rating())
////                .content(request.content())
////                .likeCount(0)
////                .createdAt(LocalDateTime.now())
////                .build();
////
////        return ReviewResponse.from(reviewRepository.save(review));
////    }
////
////    @Transactional
////    public ReviewResponse editReview(Long reviewId, Long userId, ReviewRequest request) {
////        Review review = reviewRepository.findById(reviewId)
////                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
////
////        if (!review.getUser().getId().equals(userId)) {
////            throw new ReviewAccessDeniedException(reviewId);
////        }
////
////        review.update(request.rating(), request.content()); // 엔티티에 추가할 메서드
////        return ReviewResponse.from(review); // save() 안 불러도 dirty checking으로 flush됨
////    }
////
////    @Transactional
////    public void deleteReview(Long reviewId, Long userId) {
////        Review review = reviewRepository.findById(reviewId)
////                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
////
////        if (!review.getUser().getId().equals(userId)) {
////            throw new ReviewAccessDeniedException(reviewId);
////        }
////
////        review.softDelete();
////    }
//}
