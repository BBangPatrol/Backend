package com.bbangpatrol.bakery.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;
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
            LIMIT 5
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
}
