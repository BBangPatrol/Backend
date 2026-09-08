package com.bbangpatrol.visit.repository;

import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.visit.entity.Visit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

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

    // 사용자 + 빵집 조회에 사용
    Optional<Visit> findByUserIdAndBakeryId(Long userId, Long bakeryId);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query(value = "INSERT IGNORE INTO visits (user_id, bakery_id, `count`) VALUES (:userId, :bakeryId, 0)",
            nativeQuery = true)
    void insertIfAbsent(Long userId, Long bakeryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Visit v WHERE v.user.id = :userId AND v.bakery.id = :bakeryId")
    Optional<Visit> findForUpdate(Long userId, Long bakeryId);
}
