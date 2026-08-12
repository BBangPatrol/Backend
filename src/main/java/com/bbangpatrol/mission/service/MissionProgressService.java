package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import com.bbangpatrol.point.entity.PointType;
import com.bbangpatrol.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionProgressService {

    private final MissionProgressRepository missionProgressRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public MissionProgress findByUserIdAndMissionId(Long userId, Long missionId) {
        return missionProgressRepository.findByUser_IdAndMission_Id(userId, missionId)
                .orElseThrow(() -> new ApiException(ErrorCode.MISSION_PROGRESS_NOT_FOUND));
    }

    @Transactional
    public MissionProgress receiveReward(Long userId, Long missionId) {
        MissionProgress target = findByUserIdAndMissionId(userId, missionId);

        target.receiveReward();
        userService.addPoint(
                userId,
                target.getMission().getRewardPoint(),
                PointType.earn,
                target.getMission().getTitle() + " 미션 보상"
        );
        return target;
    }
}