package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.KakaoUserInfo;
import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResult;
import com.bbangpatrol.auth.repository.RefreshTokenRepository;
import com.bbangpatrol.common.client.KakaoOAuthClient;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.util.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 가입 축하 포인트는 신규 가입 때 딱 한 번만 나가야 한다.
 * 재로그인마다 지급되면 로그아웃/로그인만 반복해도 포인트가 무한히 늘어난다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String CODE = "kakao-auth-code";
    private static final long KAKAO_ID = 12345L;
    private static final int SIGNUP_BONUS_POINT = 300;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private R2Service r2Service;
    @Mock
    private PointService pointService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("신규 가입이면 300포인트를 적립하고 내역에도 남긴다")
    void grantsSignupBonusToNewUser() {
        givenKakao();
        when(userRepository.findByKakaoId(String.valueOf(KAKAO_ID))).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user(7L));

        LoginResult result = authService.login(new LoginRequest(CODE));

        assertThat(result.isNewUser()).isTrue();
        // PointService 를 태워야 잔액과 point_history 가 함께 갱신된다
        verify(pointService).updatePoint(7L, SIGNUP_BONUS_POINT, true, "회원가입 축하 포인트");
    }

    @Test
    @DisplayName("기존 회원이 다시 로그인하면 포인트를 주지 않는다")
    void doesNotGrantBonusOnReturningLogin() {
        givenKakao();
        when(userRepository.findByKakaoId(String.valueOf(KAKAO_ID))).thenReturn(user(7L));

        LoginResult result = authService.login(new LoginRequest(CODE));

        assertThat(result.isNewUser()).isFalse();
        // 여기가 뚫리면 로그아웃/로그인 반복만으로 포인트를 무한히 벌 수 있다
        verify(pointService, never()).updatePoint(anyLong(), anyInt(), anyBoolean(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("인가 코드가 없으면 카카오를 호출하기 전에 막는다")
    void rejectsMissingAuthorizationCode() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(" ")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_KAKAO_CODE);

        verify(kakaoOAuthClient, never()).getAccessToken(anyString());
        verify(pointService, never()).updatePoint(anyLong(), anyInt(), anyBoolean(), anyString());
    }

    @Test
    @DisplayName("가입한 사용자 본인에게 지급된다")
    void grantsBonusToTheCreatedUser() {
        givenKakao();
        when(userRepository.findByKakaoId(String.valueOf(KAKAO_ID))).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user(42L));

        authService.login(new LoginRequest(CODE));

        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        verify(pointService).updatePoint(userId.capture(), anyInt(), anyBoolean(), anyString());
        assertThat(userId.getValue()).isEqualTo(42L);
    }

    private void givenKakao() {
        lenient().when(kakaoOAuthClient.getAccessToken(CODE)).thenReturn("kakao-access-token");
        lenient().when(kakaoOAuthClient.getUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserInfo(KAKAO_ID, "bbang@example.com", "빵순이"));
        lenient().when(jwtProvider.createAccessToken(anyLong())).thenReturn("access");
        lenient().when(jwtProvider.createRefreshToken(anyLong())).thenReturn("refresh");
    }

    private User user(long id) {
        return User.builder()
                .id(id)
                .name("빵순이")
                .email("bbang@example.com")
                .kakaoId(String.valueOf(KAKAO_ID))
                .pointBalance(0)
                .build();
    }
}
