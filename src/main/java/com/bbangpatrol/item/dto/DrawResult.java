package com.bbangpatrol.item.dto;

public record DrawResult(
        DrawResultResponse response,
        boolean isDuplicated
) {
}
