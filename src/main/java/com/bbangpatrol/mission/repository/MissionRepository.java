package com.bbangpatrol.mission.repository;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.entity.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // filter=all
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> com.bbangpatrol.mission.entity.MissionCriteria.NOT_SUPPORTED " +
            "ORDER BY CASE " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.not_received THEN 0 " +
            "WHEN mp.status IS NULL OR mp.status = com.bbangpatrol.mission.entity.MissionStatus.in_progress THEN 1 " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.completed THEN 2 " +
            "ELSE 3 END, m.id ASC")
    List<Object[]> findAllWithProgress(Long userId);

    // filter=in-progress (progress 없음 = 시작 안 함도 포함)
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> com.bbangpatrol.mission.entity.MissionCriteria.NOT_SUPPORTED " +
            "AND (mp.status IS NULL OR mp.status = :inProgress) " +
            "ORDER BY m.id ASC")
    List<Object[]> findInProgress(Long userId, MissionStatus inProgress);

    // filter=completed
    @Query("SELECT m, mp FROM Mission m LEFT JOIN MissionProgress mp " +
            "ON mp.mission = m AND mp.user.id = :userId " +
            "WHERE m.criteria <> com.bbangpatrol.mission.entity.MissionCriteria.NOT_SUPPORTED " +
            "AND mp.status IN :statuses " +
            "ORDER BY CASE " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.not_received THEN 0 " +
            "ELSE 1 END, m.id ASC")
    List<Object[]> findByStatuses(Long userId, List<MissionStatus> statuses);

    // 갱신 대상 미션. regions 에는 구역없음 + 행동이 일어난 지역을 넘긴다
    @Query("SELECT m FROM Mission m " +
            "WHERE m.missionType IN :types " +
            "AND m.region IN :regions " +
            "AND (m.startDate IS NULL OR m.startDate <= :today) " +
            "AND (m.endDate IS NULL OR m.endDate >= :today)")
    List<Mission> findTargets(Collection<MissionType> types, Collection<Region> regions, LocalDate today);
}
