package com.bbangpatrol.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
        public record Profile(
                @JsonProperty("nickname") String name
        ) {}
    }

    public KakaoUserInfo toUserInfo() {
        String email = (kakaoAccount != null) ? kakaoAccount.email() : null;
        String name = (kakaoAccount != null && kakaoAccount.profile() != null)
                ? kakaoAccount.profile().name() : null;
        return new KakaoUserInfo(id, email, name);
    }
}
