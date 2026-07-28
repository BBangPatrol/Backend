package com.bbangpatrol.mission.entity;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission_progress")
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

    public void receiveReward() {
        if (this.status == MissionStatus.completed) {
            throw new ApiException(ErrorCode.ALREADY_REWARDED);
        }
        if (this.status != MissionStatus.not_received) {
            throw new ApiException(ErrorCode.MISSION_NOT_COMPLETED);
        }
        this.status = MissionStatus.completed;
        this.updatedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }

    public void increaseCount() {
        if (this.status != MissionStatus.in_progress) {
            throw new IllegalStateException("더이상 횟수를 올릴 수 없습니다.");
        }
        this.count++;
        if (this.count >= mission.getTargetCount()) {
            this.count = mission.getTargetCount();
            this.status = MissionStatus.not_received;
        }
    }
}
