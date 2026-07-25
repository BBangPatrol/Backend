package com.bbangpatrol.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "카카오 인가 코드가 필요합니다.")
        String code
) {}
