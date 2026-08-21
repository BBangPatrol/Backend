package com.bbangpatrol.point.repository;

import com.bbangpatrol.point.entity.Point;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRepository extends JpaRepository<Point, Long> {
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Point> findPointHistory(Long userId, Long cursor, Pageable pageable);
}
