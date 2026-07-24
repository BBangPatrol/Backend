package com.bbangpatrol.auth.dto;

public record LoginResponse(
        String accessToken,
        boolean isNewUser
) {}
