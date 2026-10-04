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

    // 탈퇴 처리에서 쓴다. 살아 있는 리뷰만 가져와 소프트 삭제하고 평점을 다시 계산한다
    List<Review> findAllByUser_IdAndDeletedAtIsNull(Long userId);
    Long countByUserAndDeletedAtIsNull(User user);

    boolean existsByVisitDetail_IdAndDeletedAtIsNull(Long visitDetailId);

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
