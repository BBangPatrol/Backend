package com.bbangpatrol.mission.entity;

public enum MissionStatus {
    in_progress,
    not_received,
    completed,
    failed;

    public static MissionStatus fromFilter(String filter) {
        if (filter == null || filter.equals("all")) return null;
        return MissionStatus.valueOf(filter.replace("-", "_").toUpperCase());
    }
}
