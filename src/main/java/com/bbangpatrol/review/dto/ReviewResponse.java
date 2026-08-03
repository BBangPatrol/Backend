package com.bbangpatrol.review.dto;

import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewImage;
import com.bbangpatrol.review.entity.ReviewKeyword;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long writerId,
        String writerName,
        String writerImageUrl,
        BigDecimal rating,
        String content,
        List<Long> keywords,
        List<String> images,
        Integer likeCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime date
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getUser().getUserImage().getImageUrl(),
                review.getRating(),
                review.getContent(),
                review.getReviewKeywords().stream()
                        .map(ReviewKeyword::getId)
                        .toList(),
                review.getReviewImages().stream()
                        .map(ReviewImage::getImageUrl)  // ReviewImage 실제 필드명 확인
                        .toList(),
                review.getLikeCount(),
                review.getCreatedAt()
        );
    }
}