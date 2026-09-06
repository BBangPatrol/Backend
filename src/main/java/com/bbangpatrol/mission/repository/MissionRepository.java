package com.bbangpatrol.mission.repository;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.entity.MissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // filter=all
    @Query(value = "SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED " +
            "ORDER BY CASE " +
            "WHEN mp.status = MissionStatus.not_received THEN 0 " +
            "WHEN mp.status IS NULL OR mp.status = MissionStatus.in_progress THEN 1 " +
            "WHEN mp.status = MissionStatus.completed THEN 2 " +
            "ELSE 3 END, mp.updatedAt DESC NULLS LAST, m.id ASC",
            countQuery = "SELECT count(m) FROM Mission m " +
                    "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED")
    Page<Object[]> findAllWithProgress(Long userId, Pageable pageable);

    // filter=in-progress (progress 없음 = 시작 안 함도 포함)
    @Query(value = "SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED " +
            "AND (mp.status IS NULL OR mp.status = :inProgress) " +
            "ORDER BY mp.updatedAt DESC NULLS LAST, m.id ASC",
            countQuery = "SELECT count(m) FROM Mission m LEFT JOIN MissionProgress mp " +
                    "ON mp.mission = m AND mp.user.id = :userId " +
                    "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED " +
                    "AND (mp.status IS NULL OR mp.status = :inProgress)")
    Page<Object[]> findInProgress(Long userId, MissionStatus inProgress, Pageable pageable);

    // filter=completed
    @Query(value = "SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED " +
            "AND mp.status IN :statuses " +
            "ORDER BY CASE " +
            "WHEN mp.status = MissionStatus.not_received THEN 0 " +
            "ELSE 1 END, mp.updatedAt DESC NULLS LAST, m.id ASC",
            countQuery = "SELECT count(m) FROM Mission m LEFT JOIN MissionProgress mp " +
                    "ON mp.mission = m AND mp.user.id = :userId " +
                    "WHERE m.criteria <> MissionCriteria.NOT_SUPPORTED " +
                    "AND mp.status IN :statuses")
    Page<Object[]> findByStatuses(Long userId, List<MissionStatus> statuses, Pageable pageable);

    // 갱신 대상 미션. regions 에는 구역없음 + 행동이 일어난 지역을 넘긴다
    @Query("SELECT m FROM Mission m " +
            "WHERE m.missionType IN :types " +
            "AND m.region IN :regions " +
            "AND (m.startDate IS NULL OR m.startDate <= :today) " +
            "AND (m.endDate IS NULL OR m.endDate >= :today)")
    List<Mission> findTargets(Collection<MissionType> types, Collection<Region> regions, LocalDate today);
}
