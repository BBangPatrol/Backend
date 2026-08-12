package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.dto.PageInfo;
import com.bbangpatrol.mission.dto.MissionListResponse;
import com.bbangpatrol.mission.dto.MissionResponse;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private static final int PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(Long userId, String filter, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        // 미션과 미션진행도 LEFT JOIN
        List<Object[]> rows = switch (filter) {
            case "in-progress" -> missionRepository.findInProgress(userId, MissionStatus.in_progress, cursor, pageable);
            case "completed" -> missionRepository.findByStatuses(userId, List.of(MissionStatus.completed, MissionStatus.not_received), cursor, pageable);
            default -> missionRepository.findAllWithProgress(userId, cursor, pageable);
        };

        List<MissionResponse> missions = rows.stream()
                .map(row -> MissionResponse.from((Mission) row[0], (MissionProgress) row[1]))
                .toList();

        int size = missions.size();
        Long nextCursor = size == PAGE_SIZE ? missions.get(size - 1).id() : null;

        return new MissionListResponse(missions, new PageInfo(size, nextCursor != null, nextCursor));
    }
}
