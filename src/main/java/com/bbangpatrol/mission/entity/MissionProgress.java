package com.bbangpatrol.mission.entity;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mission_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mission_progress_user_mission",
                columnNames = {"user_id", "mission_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer count;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status;

    // 보상 수령 시각이 아니라 목표를 채운 시각
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    public static MissionProgress start(User user, Mission mission) {
        return MissionProgress.builder()
                .user(user)
                .mission(mission)
                .count(0)
                .status(MissionStatus.in_progress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void receiveReward() {
        if (this.status == MissionStatus.completed) {
            throw new ApiException(ErrorCode.ALREADY_REWARDED);
        }
        if (this.status != MissionStatus.not_received) {
            throw new ApiException(ErrorCode.MISSION_NOT_COMPLETED);
        }
        this.status = MissionStatus.completed;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean updateCount(int counted) {
        if (this.status != MissionStatus.in_progress) {
            return false;
        }

        int next = Math.min(counted, mission.getTargetCount());
        if (next <= this.count) {
            return false;
        }

        this.count = next;
        this.updatedAt = LocalDateTime.now();

        if (this.count >= mission.getTargetCount()) {
            this.status = MissionStatus.not_received;
            this.completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }
}
