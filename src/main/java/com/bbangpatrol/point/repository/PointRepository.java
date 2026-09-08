package com.bbangpatrol.point.repository;

import com.bbangpatrol.point.entity.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<Point, Long> {
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId ORDER BY p.id DESC")
    Page<Point> findPointHistory(@Param("userId") Long userId, Pageable pageable);
}
