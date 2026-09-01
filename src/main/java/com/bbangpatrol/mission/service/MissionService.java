package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.dto.PageInfo;
import com.bbangpatrol.mission.dto.MissionListResponse;
import com.bbangpatrol.mission.dto.MissionResponse;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(Long userId, String filter) {
        // 미션과 미션진행도 LEFT JOIN
        List<Object[]> rows = switch (filter) {
            case "in-progress" -> missionRepository.findInProgress(userId, MissionStatus.in_progress);
            case "completed" -> missionRepository.findByStatuses(userId,
                    List.of(MissionStatus.completed, MissionStatus.not_received));
            default -> missionRepository.findAllWithProgress(userId);
        };

        List<MissionResponse> missions = rows.stream()
                .map(row -> MissionResponse.from((Mission) row[0], (MissionProgress) row[1]))
                .toList();

        // 나누지 않고 다 내려준다. pageInfo 는 다른 목록 API 와 형태를 맞추기 위해 유지
        return new MissionListResponse(missions, new PageInfo(missions.size(), false, null));
    }
}
