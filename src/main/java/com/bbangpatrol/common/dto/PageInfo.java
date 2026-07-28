package com.bbangpatrol.common.dto;

public record PageInfo(
        int size,
        boolean hasNext,
        Long nextCursor
) {}
