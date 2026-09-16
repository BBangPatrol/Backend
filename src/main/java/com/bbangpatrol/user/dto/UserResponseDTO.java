package com.bbangpatrol.user.dto;

import com.bbangpatrol.bakery.dto.HotBakeryResponse;
import com.bbangpatrol.common.dto.OffsetPageInfo;
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
        String rank;
        String image;
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
        String status;
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
        OffsetPageInfo pageInfo;
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
        OffsetPageInfo pageInfo;
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
        List<VisitBakeryDTO> visits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitBakeryDTO {
        Long storeId;
        String storeName;
        String storeImageUrl;
        String visitDate;
        String state;
        Long reviewId;
        Integer rating;
        String reviewContent;
        String reviewDeadline;
        Long remainingDays;
    }
}
