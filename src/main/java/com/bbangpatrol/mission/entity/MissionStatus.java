package com.bbangpatrol.mission.entity;

/*
    in_progress : 진행중
    not_received : 미션은 완료했으나 보상을 받지 않음
    completed : 미션을 완료했고 보상도 수령함
    failed : 미션 실패
 */
public enum MissionStatus {
    in_progress,
    not_received,
    completed,
    failed
}
