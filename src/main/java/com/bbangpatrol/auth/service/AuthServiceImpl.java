package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.*;
import com.bbangpatrol.auth.repository.RefreshTokenRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.common.client.KakaoOAuthClient;
import com.bbangpatrol.user.repository.UserRepository;
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
    private final R2Service r2Service;
    private final PointService pointService;

    private static final int SIGNUP_BONUS_POINT = 300;


    // 로그인 / 회원가입 관련
    @Override
    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        if(loginRequest.code() == null || loginRequest.code().isBlank()) {
            throw new ApiException(ErrorCode.NO_KAKAO_CODE);
        }

        // 인가 코드는 로그에 남기지 않는다. 한 번 쓰면 끝나는 값이지만 교환 전에 유출되면 그대로 로그인된다
        log.info("[AuthServiceImpl] 로그인 시도 감지");

        // 인가 코드로 accessToken 가져오기
        String kakaoAccessToken = kakaoOAuthClient.getAccessToken(loginRequest.code());
        // accessToken에서 사용자 정보 가져오기
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);

        // 카카오 식별자를 통해 회원 조회
        User user = userRepository.findByKakaoIdAndDeletedAtIsNull(String.valueOf(userInfo.kakaoId()));
        boolean isNewUser = (user == null);

        if(isNewUser) { // 만약 조회되는 사용자가 없으면 회원가입이라은 뜻
            log.info("[AuthServiceImpl] 신규 유저이므로 가입 수행");
            user = userRepository.save(User.ofKakao(
                    userInfo.kakaoId(),
                    userInfo.email(),
                    userInfo.name()
            ));

            // 잔액만 올리지 않고 PointService 를 태워야 포인트 내역에도 남는다
            pointService.updatePoint(user.getId(), SIGNUP_BONUS_POINT, true, "회원가입 축하 포인트");
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
        // 리프레시 토큰 자체는 절대 로그에 남기지 않는다. 7일 유효한 값이라
        // 로그를 볼 수 있는 사람이 그대로 재발급을 받을 수 있다
        log.info("[AuthServiceImpl] 토큰 재발급 수행");

        if(!jwtProvider.validateToken(refreshToken)) { // 유효하지 않은 토큰일 경우
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰에서 userId 추출
        Long userId = jwtProvider.getUserId(refreshToken);
        log.info("[AuthServiceImpl] 토큰 재발급 대상 userId: {}", userId);

        if (!refreshTokenRepository.isValid(userId, refreshToken)) { // 기존 refresh token과 userId가 일치하지 않으면 에러
            refreshTokenRepository.delete(userId);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccess = jwtProvider.createAccessToken(userId);
        String newRefresh = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, newRefresh);

        return new ReissueResult(newAccess, newRefresh);
    }

    @Override
    public MeResponse getMe(Long userId) {
        log.info("[AuthServiceImpl] 사용자 본인 정보 조회, userId: {}", userId);

        User user = getUser(userId);

        MeResponse response = new MeResponse();
        response.setId(userId);
        response.setUserNickname(user.getName());
        response.setImageUrl(r2Service.getPublicUrl(user.getUserImage()));
        response.setPoint(user.getPointBalance());

        return response;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
