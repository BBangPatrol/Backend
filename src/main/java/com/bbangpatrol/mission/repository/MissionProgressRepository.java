package com.bbangpatrol.mission.repository;

import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MissionProgressRepository extends JpaRepository<MissionProgress, Long> {

    // 보상수령용 + 중복 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionProgress> findByUser_IdAndMission_Id(Long userId, Long missionId);

    // 없는 행만 만든다. INSERT IGNORE 라 동시 요청이 겹쳐도 유니크 제약 위반이 나지 않는다.
    // clearAutomatically 를 켜면 호출부의 영속 엔티티가 detach 되므로 false 로 둔다
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query(value = """
            INSERT IGNORE INTO mission_progress
                (user_id, mission_id, `count`, status, created_at, updated_at)
            SELECT :userId, m.id, 0, 'in_progress', NOW(), NOW()
              FROM mission m
             WHERE m.id IN (:missionIds)
            """, nativeQuery = true)
    void insertMissingProgress(Long userId, Collection<Long> missionIds);

    // 진행도 평가용 조회.
    // 잠금 조회는 스냅샷을 무시하고 최신 커밋을 읽으므로 다른 트랜잭션이 방금 만든 행도 보인다.
    // JOIN FETCH 를 붙이면 mission 행까지 잠겨 유저 간 경합이 생긴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mp FROM MissionProgress mp " +
            "WHERE mp.user.id = :userId AND mp.mission.id IN :missionIds")
    List<MissionProgress> findForUpdate(Long userId, Collection<Long> missionIds);

    @Query("SELECT mp FROM MissionProgress mp JOIN FETCH mp.mission " +
            "WHERE mp.user = :user " +
            "ORDER BY CASE " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.not_received THEN 0 " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.in_progress THEN 1 " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.completed THEN 2 " +
            "WHEN mp.status = com.bbangpatrol.mission.entity.MissionStatus.failed THEN 3 " +
            "ELSE 4 END, mp.updatedAt DESC, mp.id DESC")
    List<MissionProgress> findByUserOrderByStatusAndUpdatedAt(User user);
}
