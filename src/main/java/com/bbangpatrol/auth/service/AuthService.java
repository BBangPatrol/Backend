package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResult;
import jakarta.validation.Valid;

public interface AuthService {
    // 로그인 관련
    LoginResult login(@Valid LoginRequest loginRequest);
    // 로그아웃 관련
    void logout(Long userId, String accessToken);
}
