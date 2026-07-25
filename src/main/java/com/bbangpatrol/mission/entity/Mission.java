package com.bbangpatrol.mission.entity;

import com.bbangpatrol.common.enums.Region;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    @OneToMany(mappedBy = "mission")
    private List<MissionProgress> missionProgresses = new ArrayList<>();
}
