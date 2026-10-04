package com.bbangpatrol.common.dto;

public record OffsetPageInfo(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {}
