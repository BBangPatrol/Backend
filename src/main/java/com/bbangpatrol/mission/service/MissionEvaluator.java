package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.mission.dto.MissionResponse;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionCriteria;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;
import com.bbangpatrol.mission.entity.MissionType;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import com.bbangpatrol.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

// 사용자의 행동을 받아 관련 미션 진행도를 갱신
@Service
@Slf4j
@RequiredArgsConstructor
public class MissionEvaluator {

    private final MissionRepository missionRepository;
    private final MissionProgressRepository missionProgressRepository;
    private final MissionCounter missionCounter;

    @Transactional
    public List<MissionResponse> onReceiptVerified(Long userId, Region region) {
        return evaluate(userId, EnumSet.of(MissionType.receipt, MissionType.bakery), region);
    }

    @Transactional
    public List<MissionResponse> onReviewCreated(Long userId, Region region) {
        return evaluate(userId, EnumSet.of(MissionType.review), region);
    }

    @Transactional
    public List<MissionResponse> onItemDrawn(Long userId) {
        return evaluate(userId, EnumSet.of(MissionType.collection), Region.NONE);
    }

    private List<MissionResponse> evaluate(Long userId, Collection<MissionType> types, Region region) {
        Region target = region == null ? Region.NONE : region;

        // 구역없음 미션 + 이번 행동이 일어난 지역의 미션. 판정 불가 미션은 진행도를 만들지 않음.
        List<Mission> targets = missionRepository.findTargets(
                        types, EnumSet.of(Region.NONE, target), LocalDate.now()).stream()
                .filter(mission -> mission.getCriteria() != MissionCriteria.NOT_SUPPORTED)
                .toList();

        if (targets.isEmpty()) {
            return List.of();
        }

        Map<Long, MissionProgress> progresses = ensureProgresses(userId, targets);

        List<MissionResponse> achieved = new ArrayList<>();

        // 같은 (판정 규칙, 지역) 조합은 결과가 같으므로 한 번만 세기
        // 예) 첫 영수증 인증 / 영수증 3회 / 영수증 10회 는 모두 같은 COUNT
        Map<Entry<MissionCriteria, Region>, Integer> counted = new HashMap<>();

        for (Mission mission : targets) {
            MissionProgress progress = progresses.get(mission.getId());

            if (progress == null) {
                log.warn("[MISSION] 진행도를 찾지 못했다. userId={}, missionId={}", userId, mission.getId());
                continue;
            }

            if (progress.getStatus() != MissionStatus.in_progress) {
                continue;
            }

            int value = counted.computeIfAbsent(
                    Map.entry(mission.getCriteria(), mission.getRegion()),
                    key -> missionCounter.count(userId, mission));

            if (progress.updateCount(value)) {
                log.info("[MISSION] 미션 달성! userId={}, missionId={}, title={}",
                        userId, mission.getId(), mission.getTitle());
                achieved.add(MissionResponse.from(mission, progress));
            }
        }
        return achieved;
    }

    private Map<Long, MissionProgress> ensureProgresses(Long userId, List<Mission> targets) {
        List<Long> missionIds = targets.stream().map(Mission::getId).toList();

        missionProgressRepository.insertMissingProgress(userId, missionIds);

        return missionProgressRepository.findForUpdate(userId, missionIds).stream()
                .collect(Collectors.toMap(
                        progress -> progress.getMission().getId(),
                        progress -> progress,
                        (a, b) -> a,
                        HashMap::new));
    }
}
