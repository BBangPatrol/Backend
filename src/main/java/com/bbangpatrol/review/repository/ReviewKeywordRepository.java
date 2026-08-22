package com.bbangpatrol.review.repository;

import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;

public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {
    List<ReviewKeyword> findAllByReviewIdIn(List<Long> reviewIds);

    List<ReviewKeyword> findAllByReview(Review originalReview);
}
