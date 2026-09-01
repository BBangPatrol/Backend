package com.bbangpatrol.mission.entity;

import com.bbangpatrol.common.enums.Region;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reward_point")
    private Integer rewardPoint;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type")
    private MissionType missionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MissionCriteria criteria;

    @Column(nullable = false)
    private Region region;

    @Column(name = "target_count", nullable = false)
    private Integer targetCount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
