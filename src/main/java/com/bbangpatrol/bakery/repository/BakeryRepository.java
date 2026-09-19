package com.bbangpatrol.bakery.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BakeryRepository extends JpaRepository<Bakery, Long> {

    Optional<Bakery> findByIdAndDeletedAtIsNull(Long id);

    @Query(value = """
            WITH ranked_bakery AS (
                SELECT b.id,
                       ROW_NUMBER() OVER (
                           ORDER BY
                             CASE WHEN :sort = 'distance' THEN
                               CASE
                                 WHEN b.lat IS NULL OR b.lng IS NULL THEN 999999999
                                 ELSE ST_Distance_Sphere(POINT(b.lng, b.lat), POINT(:lon, :lat))
                               END
                             END ASC,
                             CASE WHEN :sort = 'rating' THEN b.avg_rating END DESC,
                             CASE WHEN :sort = 'visit' THEN COALESCE(SUM(v.count), 0) END DESC,
                             b.id DESC
                       ) AS sort_order
                FROM bakery b
                LEFT JOIN visits v ON v.bakery_id = b.id
                WHERE b.deleted_at IS NULL
                  AND (:name IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :name, '%')))
                GROUP BY b.id, b.lng, b.lat, b.avg_rating
            )
            SELECT id
            FROM ranked_bakery
            WHERE :cursor IS NULL
               OR sort_order > (
                    SELECT sort_order
                    FROM ranked_bakery
                    WHERE id = :cursor
               )
            ORDER BY sort_order
            """, nativeQuery = true)
    List<Long> findBakeryIdsForSearch(
            @Param("sort") String sort,
            @Param("name") String name,
            @Param("lat") BigDecimal lat,
            @Param("lon") BigDecimal lon,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query(value = """
            SELECT b.*
            FROM bakery b
            LEFT JOIN visits v ON v.bakery_id = b.id
            LEFT JOIN visit_detail vd ON vd.visit_id = v.id
            WHERE b.deleted_at IS NULL
            GROUP BY b.id
            ORDER BY COUNT(vd.id) DESC,
                     MAX(vd.visited_at) DESC,
                     b.id DESC
            LIMIT 4
            """, nativeQuery = true)
    List<Bakery> findHotBakeries();

    @Query("""
            select b.businessNumber
            from Bakery b
            where b.id = :storeId
            """)
    Optional<String> findBusinessNumberByStoreId(
            @Param("storeId") Long storeId
    );

    /**
     * 평점은 리뷰에서 계산하지 않고 bakery.avg_rating 에 저장된 값을 그대로 내려준다(V9 시드 주석 참고).
     * 그래서 리뷰가 바뀔 때마다 이 메서드로 그 행을 다시 채운다.
     * 살아 있는 리뷰가 하나도 없으면 NULL(별점 없음)이 된다.
     *
     * 평균을 애플리케이션으로 읽어 와 다시 쓰지 않고 UPDATE 한 문장으로 계산하는 이유:
     * 같은 가게에 리뷰가 동시에 달려도 UPDATE 가 bakery 행을 잠그고 최신 커밋된 리뷰를 보고
     * 계산하므로, 한쪽 리뷰만 반영된 값이 남지 않는다.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE bakery b
            SET b.avg_rating = (SELECT ROUND(AVG(r.rating), 1)
                                FROM review r
                                WHERE r.bakery_id = b.id
                                  AND r.deleted_at IS NULL),
                b.updated_at = NOW()
            WHERE b.id = :bakeryId
            """, nativeQuery = true)
    void refreshAvgRating(@Param("bakeryId") long bakeryId);
}
