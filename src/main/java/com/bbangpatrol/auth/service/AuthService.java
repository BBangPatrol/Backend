package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResponse;
import jakarta.validation.Valid;

public interface AuthService {
    // 로그인 관련
    LoginResponse login(@Valid LoginRequest loginRequest);
}
