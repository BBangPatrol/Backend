package com.bbangpatrol.review.repository;

import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    @Query("SELECT COUNT(rl) FROM ReviewLike rl JOIN Review r ON rl.review = r WHERE r.user = :user")
    Long countLikes(User user);


    ReviewLike findByUserAndReview(User user, Review review);

    @Query("SELECT rl.review.id FROM ReviewLike rl WHERE rl.user.id = :userId AND rl.review.id IN :reviewIds")
    List<Long> findLikedReviewIds(@Param("userId") long userId, @Param("reviewIds") Collection<Long> reviewIds);
}
