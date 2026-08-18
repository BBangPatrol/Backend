package com.bbangpatrol.review.repository;

import com.bbangpatrol.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findAllByReviewIdIn(List<Long> reviewIds);
}
