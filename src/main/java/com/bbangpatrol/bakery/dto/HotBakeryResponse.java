package com.bbangpatrol.bakery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class HotBakeryResponse {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BakeryListDTO {
        List<BakerySimpleDTO> stores;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BakerySimpleDTO {
        Long storeId;
        String storeName;
        String imageUrl;
        BigDecimal rating;
        String region;
    }
}