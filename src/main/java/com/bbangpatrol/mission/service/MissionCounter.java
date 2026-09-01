package com.bbangpatrol.mission.service;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.repository.MissionCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MissionCounter {

    private final MissionCountRepository missionCountRepository;

    public int count(Long userId, Mission mission) {
        long counted = switch (mission.getCriteria()) {
            case RECEIPT_COUNT -> mission.getRegion() == Region.NONE
                    ? missionCountRepository.countReceipts(userId)
                    : missionCountRepository.countReceiptsByRegion(userId, mission.getRegion());
            case DISTINCT_REGION -> missionCountRepository.countDistinctRegions(userId, Region.NONE);
            case DISTINCT_BAKERY -> missionCountRepository.countDistinctBakeries(userId);
            case REVIEW_COUNT -> missionCountRepository.countReviews(userId);
            case DISTINCT_ITEM -> missionCountRepository.countDistinctItems(userId);
            case NOT_SUPPORTED -> 0L; // 판정 불가. 진행도를 올리지 않는다
        };
        return (int) counted;
    }
}
