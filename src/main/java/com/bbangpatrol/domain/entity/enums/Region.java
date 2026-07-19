package com.bbangpatrol.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Region {
    YUSEONG("유성구"),
    SEO("서구"),
    DAEDEOK("대덕구"),
    JUNG("중구"),
    DONG("동구"),
    NONE("구역없음");

    private final String value;
}
