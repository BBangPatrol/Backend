package com.bbangpatrol.mission.dto;

import com.bbangpatrol.mission.entity.Mission;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.mission.entity.MissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MissionResponse(
        Long id,
        String title,
        String description,
        int count,
        int targetCount,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime completeDate,
        String status,
        String missionType
) {
    public static MissionResponse from(Mission mission, MissionProgress progress) {
        int count = 0;
        LocalDateTime completedAt = null;
        String status = MissionStatus.not_received.name();

        if (progress != null) {
            count = progress.getCount();
            completedAt = progress.getCompletedAt();
            status = progress.getStatus().name();
        }

        return new MissionResponse(
                mission.getId(), mission.getTitle(), mission.getDescription(),
                count, mission.getTargetCount(),
                mission.getStartDate(), mission.getEndDate(),
                completedAt, status, mission.getMissionType().name()
        );
    }
}