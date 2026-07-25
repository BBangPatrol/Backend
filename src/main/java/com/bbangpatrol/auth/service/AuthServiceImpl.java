package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.KakaoUserInfo;
import com.bbangpatrol.auth.repository.RefreshTokenRepository;
import com.bbangpatrol.auth.repository.UserRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.common.client.KakaoOAuthClient;
import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResult;
import com.bbangpatrol.util.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;


    // 로그인 / 회원가입 관련
    @Override
    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        log.info("[AuthServiceImpl] 로그인 시도 감지, 카카오 인가 코드: {}", loginRequest.code());

        // 인가 코드로 accessToken 가져오기
        String kakaoAccessToken = kakaoOAuthClient.getAccessToken(loginRequest.code());
        // accessToken에서 사용자 정보 가져오기
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);

        // 카카오 식별자를 통해 회원 조회
        User user = userRepository.findByKakaoId(String.valueOf(userInfo.kakaoId()));
        boolean isNewUser = (user == null);

        if(isNewUser) { // 만약 조회되는 사용자가 없으면 회원가입이라은 뜻
            log.info("[AuthServiceImpl] 신규 유저이므로 가입 수행");
            user = userRepository.save(User.ofKakao(
                    userInfo.kakaoId(),
                    userInfo.email(),
                    userInfo.name()
            ));
        }

        // 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(user.getId(), refreshToken);

        return new LoginResult(accessToken, refreshToken, isNewUser); // 토큰 두 개랑, 신규 유저 여부 리턴
    }
}
