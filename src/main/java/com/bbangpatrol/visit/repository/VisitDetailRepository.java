package com.bbangpatrol.visit.repository;

import com.bbangpatrol.visit.entity.VisitDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitDetailRepository extends JpaRepository<VisitDetail, Long> {

    boolean existsByReceiptHash(String receiptHash);

    @Query("""
        select count(vd) > 0 from VisitDetail vd
        where vd.receiptHash = :receiptHash
          and vd.visit.user.id = :userId
    """)
    boolean existsByReceiptHashAndUserId(@Param("receiptHash") String receiptHash,
                                         @Param("userId") Long userId);

    @Query("""
        select vd from VisitDetail vd
        join fetch vd.visit v
        join fetch v.user
        join fetch v.bakery
        where vd.id = :visitDetailId
    """)
    Optional<VisitDetail> findByIdWithVisit(@Param("visitDetailId") Long visitDetailId);

    @Query("""
        select vd from VisitDetail vd
        where vd.visit.user.id = :userId
          and vd.visit.bakery.id = :bakeryId
          and not exists (
              select 1 from Review r
              where r.visitDetail = vd
                and r.deletedAt is null
          )
        order by vd.visitedAt desc, vd.id desc
    """)
    List<VisitDetail> findReviewable(@Param("userId") Long userId,
                                     @Param("bakeryId") Long bakeryId,
                                     Pageable pageable);

    @Query("""
        select vd from VisitDetail vd
        join fetch vd.visit v
        join fetch v.bakery b
        left join fetch vd.review
        where v.user.id = :userId
          and (:query = '' or b.name like concat('%', :query, '%'))
        order by vd.visitedAt desc, vd.id desc
    """)
    List<VisitDetail> findHistoryByUserId(@Param("userId") Long userId, @Param("query") String query);
}
