package com.bbangpatrol.visit.repository;

import com.bbangpatrol.visit.entity.VisitDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitDetailRepository extends JpaRepository<VisitDetail, Long> {

    boolean existsByReceiptHash(String receiptHash);

    // 시연용으로 한 영수증을 여러 사람이 쓰게 열어둘 때 쓴다. 같은 사람의 재사용은 그대로 막힌다
    @Query("""
        select count(vd) > 0 from VisitDetail vd
        where vd.receiptHash = :receiptHash
          and vd.visit.user.id = :userId
    """)
    boolean existsByReceiptHashAndUserId(@Param("receiptHash") String receiptHash,
                                         @Param("userId") Long userId);

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

    // 한 방에 가져온다. VisitDetail.review 는 역방향 @OneToOne 이라 LAZY 여도 건건이 조회된다
    // query 가 빈 문자열이면 가게 이름 조건을 건너뛴다
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
