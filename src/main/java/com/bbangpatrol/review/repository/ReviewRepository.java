package com.bbangpatrol.review.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Long countByUser(User user);
    Long countByUserAndDeletedAtIsNull(User user);

    @Query("SELECT COALESCE(SUM(r.likeCount), 0) FROM Review r WHERE r.user = :user AND r.deletedAt IS NULL")
    Long sumLikeCountByUser(User user);

    @Query("SELECT r FROM Review r JOIN FETCH r.bakery WHERE r.user.id = :userId AND r.deletedAt IS NULL AND (:cursor IS NULL OR r.id < :cursor) ORDER BY r.id DESC")
    List<Review> findMyReviews(Long userId, Long cursor, Pageable pageable);

    List<Review> findAllByBakery(Bakery bakery);

    // cursor가 null인 경우 => 첫 페이지
    @Query("""
        select r from Review r
        join fetch r.user
        where r.bakery.id = :bakeryId
        and r.deletedAt is null 
        and (:cursor is null  or r.id < :cursor)
        order by r.id desc 
    """)
    List<Review> findAllByBakeryWithCursor(
            @Param("bakeryId") long bakeryId,
            @Param("cursor") long cursor,
            Pageable pageable);
}
