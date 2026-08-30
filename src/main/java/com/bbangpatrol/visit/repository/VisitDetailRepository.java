package com.bbangpatrol.visit.repository;

import com.bbangpatrol.visit.entity.VisitDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDetailRepository extends JpaRepository<VisitDetail, Long> {

    boolean existsByReceiptHash(String receiptHash);
}