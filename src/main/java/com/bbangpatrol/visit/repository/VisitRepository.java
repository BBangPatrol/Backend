package com.bbangpatrol.visit.repository;

import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.visit.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUser(User user);

    // 사용자 + 빵집 조회에 사용
    Optional<Visit> findByUserIdAndBakeryId(Long userId, Long bakeryId);
}
