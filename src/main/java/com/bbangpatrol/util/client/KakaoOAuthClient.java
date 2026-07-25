package com.bbangpatrol.util.client;

import com.bbangpatrol.auth.dto.KakaoUserInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    public String getAccessToken(@NotBlank(message = "카카오 인가 코드가 필요합니다.") String code) {

        return null;
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {

        return null;
    }
}
