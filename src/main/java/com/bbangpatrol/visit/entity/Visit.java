package com.bbangpatrol.visit.entity;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "visits")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bakery_id")
    private Bakery bakery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "visit")
    private List<VisitDetail> visitDetails = new ArrayList<>();

    public void increaseCount() { // 사용자 단일 빵집 방문 횟수 증가
        if (this.count == null) {
            this.count = 1;
            return;
        }

        this.count++;
    }

    public static Visit create(User user, Bakery bakery) {
        return Visit.builder()
                .user(user)
                .bakery(bakery)
                .count(0)
                .build();
    }
}
