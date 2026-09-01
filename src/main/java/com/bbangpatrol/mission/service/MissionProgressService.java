package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.dto.MissionRewardResponse;
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

    @Transactional
    public MissionRewardResponse receiveReward(Long userId, Long missionId) {
        MissionProgress target = missionProgressRepository
                .findByUser_IdAndMission_Id(userId, missionId)
                .orElseThrow(() -> new ApiException(ErrorCode.MISSION_PROGRESS_NOT_FOUND));

        target.receiveReward();

        Integer rewardPoint = target.getMission().getRewardPoint();
        int earnPoint = rewardPoint == null ? 0 : rewardPoint;

        Integer totalPoint = userService.addPoint(
                userId,
                earnPoint,
                PointType.earn,
                target.getMission().getTitle() + " 미션 보상"
        );
        return new MissionRewardResponse(earnPoint, totalPoint);
    }
}
