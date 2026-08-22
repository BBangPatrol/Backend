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
        LocalDateTime completedDate,
        String status,
        String missionType
) {
    public static MissionResponse from(Mission mission, MissionProgress progress) {
        int count = 0;
        LocalDateTime completedAt = null;
        MissionStatus status = MissionStatus.in_progress;

        if (progress != null) {
            count = progress.getCount();
            completedAt = progress.getCompletedAt();
            status = progress.getStatus();
        }

        return new MissionResponse(
                mission.getId(), mission.getTitle(), mission.getDescription(),
                count, mission.getTargetCount(),
                mission.getStartDate(), mission.getEndDate(),
                completedAt, toCamelCase(status), mission.getMissionType().name()
        );
    }

    private static String toCamelCase(MissionStatus status) {
        return switch (status) {
            case in_progress -> "inProgress";
            case not_received -> "notReceived";
            case completed -> "completed";
            case failed -> "failed";
        };
    }
}