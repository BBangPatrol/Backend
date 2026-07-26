package com.bbangpatrol.auth.dto;

public record ReissueResult(
        String accessToken,
        String refreshToken
) {
}
