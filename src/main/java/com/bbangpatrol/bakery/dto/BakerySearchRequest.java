package com.bbangpatrol.bakery.dto;

// 거리순(sort=distance, lat/lon)은 폐기했다. 프론트가 위치를 보내지 않아 쓰인 적이 없고,
// 지도 목록은 방문자순/평점순만 제공한다
public record BakerySearchRequest(
        String sort,
        String name,
        Long cursor,
        Boolean favoriteOnly
) {

    public BakerySearchRequest {
        if (sort == null || sort.isBlank()) {
            sort = "visit";
        }
        if (favoriteOnly == null) {
            favoriteOnly = false;
        }
    }
}
