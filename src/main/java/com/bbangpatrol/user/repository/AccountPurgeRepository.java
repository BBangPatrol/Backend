package com.bbangpatrol.user.repository;

import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보관 기간이 지난 탈퇴 회원과 삭제된 리뷰를 DB 에서 실제로 지운다.
 *
 * 탈퇴는 soft delete(user.deleted_at)라 행이 그대로 남는다.
 * 개인정보처리방침이 "탈퇴 후 30일 뒤 파기"를 약속하므로, 그 약속을 실행하는 자리가 여기다.
 *
 * FK 때문에 지우는 순서가 정해져 있다 (자식 → 부모).
 *   review_like / review_image / review_keyword → review → visit_detail → visits → 나머지 → user
 * JPQL 로는 조인 삭제가 안 돼서 네이티브 쿼리를 쓴다.
 */
public interface AccountPurgeRepository extends Repository<User, Long> {

    @Query(value = """
            SELECT id FROM user
             WHERE deleted_at IS NOT NULL
               AND deleted_at < :cutoff
            """, nativeQuery = true)
    List<Long> findExpiredUserIds(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = """
            SELECT id FROM review
             WHERE deleted_at IS NOT NULL
               AND deleted_at < :cutoff
            """, nativeQuery = true)
    List<Long> findExpiredReviewIds(@Param("cutoff") LocalDateTime cutoff);

    // R2 에 남는 파일은 DB 행을 지우면 참조가 끊기므로, 지우기 전에 key 를 모아둔다
    @Query(value = """
            SELECT image_url FROM review_image WHERE review_id IN (:reviewIds)
            UNION ALL
            SELECT thumbnail_url FROM review_image WHERE review_id IN (:reviewIds) AND thumbnail_url IS NOT NULL
            """, nativeQuery = true)
    List<String> findReviewImageKeys(@Param("reviewIds") List<Long> reviewIds);

    @Query(value = "SELECT user_image FROM user WHERE id IN (:userIds) AND user_image IS NOT NULL",
            nativeQuery = true)
    List<String> findUserImageKeys(@Param("userIds") List<Long> userIds);

    @Query(value = "SELECT id FROM review WHERE user_id IN (:userIds)", nativeQuery = true)
    List<Long> findReviewIdsByUserIds(@Param("userIds") List<Long> userIds);

    // ----- 리뷰에 매달린 것들 -----

    @Modifying
    @Query(value = "DELETE FROM review_like WHERE review_id IN (:reviewIds)", nativeQuery = true)
    int deleteReviewLikesByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Modifying
    @Query(value = "DELETE FROM review_image WHERE review_id IN (:reviewIds)", nativeQuery = true)
    int deleteReviewImagesByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Modifying
    @Query(value = "DELETE FROM review_keyword WHERE review_id IN (:reviewIds)", nativeQuery = true)
    int deleteReviewKeywordsByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Modifying
    @Query(value = "DELETE FROM review WHERE id IN (:reviewIds)", nativeQuery = true)
    int deleteReviewsByIds(@Param("reviewIds") List<Long> reviewIds);

    // ----- 회원에 매달린 것들 -----

    // 탈퇴 회원이 남의 리뷰에 누른 좋아요
    @Modifying
    @Query(value = "DELETE FROM review_like WHERE user_id IN (:userIds)", nativeQuery = true)
    int deleteReviewLikesByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = """
            DELETE vd FROM visit_detail vd
              JOIN visits v ON v.id = vd.visit_id
             WHERE v.user_id IN (:userIds)
            """, nativeQuery = true)
    int deleteVisitDetailsByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM visits WHERE user_id IN (:userIds)", nativeQuery = true)
    int deleteVisitsByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM bookmark WHERE user_id IN (:userIds)", nativeQuery = true)
    int deleteBookmarksByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM user_item WHERE user_id IN (:userIds)", nativeQuery = true)
    int deleteUserItemsByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM mission_progress WHERE user_id IN (:userIds)", nativeQuery = true)
    int deleteMissionProgressByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM point_history WHERE user_id IN (:userIds)", nativeQuery = true)
    int deletePointHistoryByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "DELETE FROM user WHERE id IN (:userIds)", nativeQuery = true)
    int deleteUsersByIds(@Param("userIds") List<Long> userIds);
}
