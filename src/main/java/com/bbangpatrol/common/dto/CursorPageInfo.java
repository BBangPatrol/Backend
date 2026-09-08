package com.bbangpatrol.common.dto;

public record CursorPageInfo(
        int size,
        boolean hasNext,
        Long nextCursor
) {}
