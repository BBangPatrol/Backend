package com.bbangpatrol.common.exception;

public record ErrorDetail(
        String field,
        String message
) {
}
