package com.bbangpatrol.mission.repository;

import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionProgressRepository extends JpaRepository<MissionProgress, Long> {

    // 동시에 두 번 요청 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mp FROM MissionProgress mp JOIN FETCH mp.mission " +
            "WHERE mp.user.id = :userId " +
            "AND (:status IS NULL OR mp.status = :status) " +
            "AND (:cursor IS NULL OR mp.id > :cursor) " +
            "ORDER BY mp.id ASC")
    List<MissionProgress> findMissions(Long userId, MissionStatus status, Long cursor, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionProgress> findByUser_IdAndMission_Id(Long userId, Long missionId);
}
