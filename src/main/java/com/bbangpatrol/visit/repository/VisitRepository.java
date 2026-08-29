package com.bbangpatrol.visit.repository;

import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.visit.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUser(User user);

    @Query("""
            select v.bakery.id, coalesce(sum(v.count), 0)
            from Visit v
            where v.bakery.id in :bakeryIds
            group by v.bakery.id
            """)
    List<Object[]> sumVisitCountsByBakeryIds(@Param("bakeryIds") List<Long> bakeryIds);

    @Query("""
            select coalesce(sum(v.count), 0)
            from Visit v
            where v.bakery.id = :bakeryId
            """)
    long sumVisitCountByBakeryId(@Param("bakeryId") Long bakeryId);
}
