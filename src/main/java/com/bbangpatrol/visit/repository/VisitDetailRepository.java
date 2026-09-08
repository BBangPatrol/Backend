package com.bbangpatrol.visit.repository;

import com.bbangpatrol.visit.entity.VisitDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitDetailRepository extends JpaRepository<VisitDetail, Long> {

    boolean existsByReceiptHash(String receiptHash);

    // vd.review is null 로는 소프트 삭제를 못 걸러 not exists 를 쓴다
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

}
