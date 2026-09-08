package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.dto.OffsetPageable;
import com.bbangpatrol.common.dto.CursorPageInfo;
import com.bbangpatrol.mission.dto.MissionListResponse;
import com.bbangpatrol.mission.dto.MissionMainResponse;
import com.bbangpatrol.mission.dto.MissionResponse;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    // 메인 화면에 노출하는 미션 수. 화면이 고정 4칸이다
    private static final int MAIN_SIZE = 4;

    private final MissionRepository missionRepository;

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(Long userId, String filter, Long cursor, Integer size) {
        // 정렬 기준이 미션 상태라 id 기반 keyset 이 성립하지 않는다.
        // cursor 는 FE 가 그대로 돌려주는 불투명 토큰이고, 서버는 "건너뛴 개수" 로 해석한다.
        // 페이지 번호가 아니라 offset 이라 화면이 바뀌어 size 가 달라져도 위치가 어긋나지 않는다.
        int limit = Math.min(Math.max(size == null ? DEFAULT_SIZE : size, 1), MAX_SIZE);
        long offset = cursor == null ? 0 : Math.max(cursor, 0);
        Pageable pageable = new OffsetPageable(offset, limit);

        // 미션과 미션진행도 LEFT JOIN
        Page<Object[]> rows = switch (filter) {
            case "in-progress" -> missionRepository.findInProgress(userId, MissionStatus.in_progress, pageable);
            case "completed" -> missionRepository.findByStatuses(userId,
                    List.of(MissionStatus.completed, MissionStatus.not_received), pageable);
            default -> missionRepository.findAllWithProgress(userId, pageable);
        };

        List<MissionResponse> missions = rows.getContent().stream()
                .map(row -> MissionResponse.from((Mission) row[0], (MissionProgress) row[1]))
                .toList();

        // offset 이 limit 의 배수가 아닐 수 있어 Page.hasNext() 대신 직접 계산한다
        long consumed = offset + missions.size();
        boolean hasNext = consumed < rows.getTotalElements();
        Long nextCursor = hasNext ? consumed : null;

        return new MissionListResponse(missions, new CursorPageInfo(missions.size(), hasNext, nextCursor));
    }

    /**
     * 메인 화면용 미션. 목록의 filter=all 과 같은 정렬(보상 수령 가능 -> 진행 중 -> 완료 -> 실패)에서
     * 상위 4개만 잘라 준다. 개수가 고정이라 페이징 파라미터를 받지 않는다.
     */
    @Transactional(readOnly = true)
    public MissionMainResponse getMainMissions(Long userId) {
        List<MissionResponse> missions =
                missionRepository.findMainMissions(userId, PageRequest.of(0, MAIN_SIZE)).stream()
                        .map(row -> MissionResponse.from((Mission) row[0], (MissionProgress) row[1]))
                        .toList();

        return new MissionMainResponse(missions);
    }
}
