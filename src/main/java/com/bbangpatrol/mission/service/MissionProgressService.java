package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.dto.MissionRewardResponse;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionProgressService {

    private final MissionProgressRepository missionProgressRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional
    public MissionRewardResponse receiveReward(Long userId, Long missionId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        MissionProgress target = missionProgressRepository
                .findByUser_IdAndMission_Id(userId, missionId)
                .orElseThrow(() -> new ApiException(ErrorCode.MISSION_PROGRESS_NOT_FOUND));

        target.receiveReward();

        Integer rewardPoint = target.getMission().getRewardPoint();
        int earnPoint = rewardPoint == null ? 0 : rewardPoint;

        pointService.updatePoint(userId, earnPoint, true, target.getMission().getTitle() + " 미션 보상");

        return new MissionRewardResponse(earnPoint, user.getPointBalance());
    }
}
