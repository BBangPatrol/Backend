package com.bbangpatrol.user.dto;

import com.bbangpatrol.common.dto.PageInfo;
import com.bbangpatrol.item.entity.ItemRank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        Long collectibleId;
        String name;
        ItemRank rank;
        String image;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointHistoryDTO {
        List<PointDTO> point_history;
        PageInfo pageInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointDTO {
        String type;
        String content;
        Integer amount;
        LocalDateTime date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewHistoryDTO {
        List<ReviewDTO> reviews;
        Long reviewCount;
        Long reviewLikes;
        PageInfo pageInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewDTO {
        Long bakeryId;
        String bakeryName;
        Integer rating;
        String content;
        Integer likeCount;
        LocalDateTime date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitedBakeryListDTO {
        List<Coordinate> visits;
    }
}
