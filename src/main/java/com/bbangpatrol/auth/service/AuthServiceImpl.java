package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.KakaoUserInfo;
import com.bbangpatrol.auth.dto.ReissueResult;
import com.bbangpatrol.auth.repository.RefreshTokenRepository;
import com.bbangpatrol.auth.repository.UserRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.common.client.KakaoOAuthClient;
import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResult;
import com.bbangpatrol.util.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;


    // 로그인 / 회원가입 관련
    @Override
    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        if(loginRequest.code() == null || loginRequest.code().isBlank()) {
            throw new ApiException(ErrorCode.NO_KAKAO_CODE);
        }

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

    // 로그아웃 관련
    @Override
    public void logout(Long userId, String accessToken) {
        log.info("[AuthServiceImpl] 로그아웃 수행, userId: {}", userId);

        // 기존의 refresh token redis에서 삭제
        if (userId != null) refreshTokenRepository.delete(userId);

        if (accessToken != null) {
            long remaining = jwtProvider.getExpiration(accessToken);
            if (remaining > 0) { // 기존에 남은 시간만큼을 유효기간으로 해서 redis에 블랙리스트로 등록
                redisTemplate.opsForValue().set(
                        "BL:" + accessToken,
                        "logout", // 사유는 로그아웃
                        Duration.ofMillis(remaining)
                );
            }
        }
    }

    // 토큰 재발급 관련
    @Override
    public ReissueResult reissue(String refreshToken) {
        log.info("[AuthServiceImpl] 토큰 재발급 수행, 기존의 refresh token: {}", refreshToken);

        if(!jwtProvider.validateToken(refreshToken)) { // 유효하지 않은 토큰일 경우
            throw new RuntimeException();
        }

        // 토큰에서 userId 추출
        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenRepository.isValid(userId, refreshToken)) { // 기존 refresh token과 userId가 일치하지 않으면 에러
            refreshTokenRepository.delete(userId);
            throw new RuntimeException();
        }

        String newAccess = jwtProvider.createAccessToken(userId);
        String newRefresh = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, newRefresh);

        return new ReissueResult(newAccess, newRefresh);
    }
}
