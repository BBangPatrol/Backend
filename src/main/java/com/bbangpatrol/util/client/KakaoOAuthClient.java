package com.bbangpatrol.util.client;

import com.bbangpatrol.auth.dto.KakaoTokenResponse;
import com.bbangpatrol.auth.dto.KakaoUserInfo;
import com.bbangpatrol.auth.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthClient {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token"; // 인가 코드를 카카오 엑세스 토큰으로 교환해주는 주소
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";// 발급 받은 엑세스 토큰으로 사용자 정보를 조회하는 주소

    private final RestClient restClient = RestClient.create();

    public String getAccessToken(String code) { // 프론트에서 전달 받은 인가 코드를 카카오 access token으로 교환하는 메서드
        log.info("[KakaoOAuthClient] 인가 코드를 카카오 access token으로 교환 시작 code: {}", code);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        KakaoTokenResponse response = restClient.post() // token uri로 form 내용 전달
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    log.error("[KakaoOAuthClient] 토큰 발급 실패: {}", res.getStatusCode());
                    // 이 부분 추후에 401로 변경 필요
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 토큰 발급에 실패했습니다.");
                })
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            // 이 부분도 502로 변경 필요
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 토큰 응답이 올바르지 않습니다.");
        }
        return response.accessToken();
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        KakaoUserResponse response = restClient.get()
                .uri(USER_INFO_URI)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    log.error("[KakaoOAuthClient] 사용자 조회 실패: {}", res.getStatusCode());
                    // 카카오 통신 실패 502
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다.");
                })
                .body(KakaoUserResponse.class);

        if (response == null || response.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 사용자 응답이 올바르지 않습니다.");
        }
        return response.toUserInfo();
    }
}
