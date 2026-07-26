package com.bbangpatrol.auth.dto;

public record LoginResult(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {}