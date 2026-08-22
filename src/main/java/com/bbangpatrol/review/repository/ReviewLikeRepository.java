package com.bbangpatrol.review.repository;

import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    @Query("SELECT COUNT(rl) FROM ReviewLike rl JOIN Review r ON rl.review = r WHERE r.user = :user")
    Long countLikes(User user);


    ReviewLike findByUserAndReview(User user, Review review);
}
