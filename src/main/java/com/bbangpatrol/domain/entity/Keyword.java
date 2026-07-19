package com.bbangpatrol.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "keyword")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=30)
    private String label;

    @Builder.Default
    @OneToMany(mappedBy = "keyword")
    private List<ReviewKeyword> reviewKeywords = new ArrayList<>();
}
