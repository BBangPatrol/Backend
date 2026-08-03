package com.bbangpatrol.mission.repository;

import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // filter=all
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE (:cursor IS NULL OR m.id > :cursor) ORDER BY mp.updatedAt ASC")
    List<Object[]> findAllWithProgress(Long userId, Long cursor, Pageable pageable);

    // filter=in-progress (progress 없음 = 시작 안 함도 포함)
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE (mp IS NULL OR mp.status = :inProgress) " +
            "AND (:cursor IS NULL OR m.id > :cursor) ORDER BY mp.updatedAt ASC")
    List<Object[]> findInProgress(Long userId, MissionStatus inProgress, Long cursor, Pageable pageable);

    // filter=completed
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE mp.status IN :statuses " +
            "AND (:cursor IS NULL OR m.id > :cursor) ORDER BY mp.updatedAt ASC")
    List<Object[]> findByStatuses(Long userId, List<MissionStatus> statuses, Long cursor, Pageable pageable);
}