package com.bbangpatrol.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.math.BigDecimal;
import java.util.List;

public class UserResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPageDTO {
        String nickname;
        CollectionBook collectionBooks;
        List<Coordinate> map;
        Integer point;
        ReviewStat reviews;
        List<MissionDTO> missions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionBook {
        Integer collected;
        Integer total;
        List<CollectionItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItem {
        Long id;
        String name;
        String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinate {
        BigDecimal lat;
        BigDecimal lon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewStat {
        Long reviewCount;
        Long reviewLikes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionDTO {
        Long missionId;
        String title;
        Integer count;
        Integer targetCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileImageDTO {
        String imageUrl;
    }
}
