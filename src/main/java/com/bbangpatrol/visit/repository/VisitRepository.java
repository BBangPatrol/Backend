package com.bbangpatrol.visit.repository;

import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.visit.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUser(User user);
}
