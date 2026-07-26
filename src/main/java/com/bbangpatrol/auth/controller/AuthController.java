package com.bbangpatrol.auth.controller;

import com.bbangpatrol.auth.dto.*;
import com.bbangpatrol.auth.service.AuthService;
import com.bbangpatrol.common.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login") // 로그인
    ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest loginRequest) {

        LoginResult result = authService.login(loginRequest);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        // refresh token은 제외한 access token과 isNewUser 여부만 body에 담아서 리턴
        LoginResponse response = new LoginResponse(result.accessToken(), result.isNewUser());

        if(response.isNewUser()) { // 신규 유저일 경우에는 201 리턴
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(ApiResponse.onSuccess(
                            HttpStatus.CREATED,
                            "회원가입에 성공했습니다.",
                            response
                    ));
        }
        return ResponseEntity.status(HttpStatus.OK) // 기존 유저일 경우에는 200 리턴
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "로그인에 성공했습니다.",
                        response
                ));
    }

    @PostMapping("/logout") // 로그아웃
    ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal Long userId) {

        String bearerToken = request.getHeader("Authorization");
        String accessToken = null;

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);  // "Bearer " 7글자 제거
        }
        // 로그아웃 요청 처리
        authService.logout(userId, accessToken);

        // refresh 쿠키 제거
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "로그아웃에 성공했습니다.",
                        null // 데이터는 없음
                ));
    }

    // access token 재발급
    @PostMapping("/reissue")
    ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        ReissueResult result = authService.reissue(refreshToken);

        // 새로 발급받은 refresh token은 header에 담기
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();
        // 쿠키에 refresh token 저장
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        // 새로 발급 받은 access token 담기
        ReissueResponse data = new ReissueResponse(result.accessToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "토큰 재발급에 성공했습니다.",
                        data));
    }
}
