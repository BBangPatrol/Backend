package com.bbangpatrol.review.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Long countByUserAndDeletedAtIsNull(User user);

    @Query("SELECT COALESCE(SUM(r.likeCount), 0) FROM Review r WHERE r.user = :user AND r.deletedAt IS NULL")
    Long sumLikeCountByUser(User user);

    @Query(
            value = """
                    SELECT r FROM Review r
                    JOIN FETCH r.bakery
                    WHERE r.user.id = :userId
                    AND r.deletedAt IS NULL
                    ORDER BY r.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM Review r
                    WHERE r.user.id = :userId
                    AND r.deletedAt IS NULL
                    """)
    Page<Review> findMyReviews(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    select r from Review r
                    join fetch r.user
                    where r.bakery.id = :bakeryId
                    and r.deletedAt is null
                    order by r.id desc
                    """,
            countQuery = """
                    select count(r) from Review r
                    where r.bakery.id = :bakeryId
                    and r.deletedAt is null
                    """)
    Page<Review> findPageByBakeryId(@Param("bakeryId") long bakeryId, Pageable pageable);

}
