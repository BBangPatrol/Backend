package com.bbangpatrol.review.repository;

import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Long countByUser(User user);
}
