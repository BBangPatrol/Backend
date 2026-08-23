package com.bbangpatrol.bakery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AttractionCategory {
    TOURIST_SPOT("12", "관광지"),
    CULTURAL_FACILITY("14", "문화시설"),
    EVENT("15", "축제공연행사"),
    LEISURE_SPORTS("28", "레포츠"),
    ETC("", "기타");

    private final String contentTypeId;
    private final String label;

    public static AttractionCategory fromContentTypeId(String contentTypeId) {
        return Arrays.stream(values())
                .filter(category -> category.contentTypeId.equals(contentTypeId))
                .findFirst()
                .orElse(ETC);
    }
}
