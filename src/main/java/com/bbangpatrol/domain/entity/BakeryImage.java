package com.bbangpatrol.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bakery_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BakeryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob private String origin;

    @Lob
    @Column(name = "imageUrl")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bakery_id")
    private Bakery bakery;
}
