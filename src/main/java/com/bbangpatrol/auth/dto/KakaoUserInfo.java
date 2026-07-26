package com.bbangpatrol.auth.dto;

public record KakaoUserInfo(
        Long kakaoId, // 사용자 ID
        String email, // 사용자 이메일
        String name   // 사용자 이름
) {}
