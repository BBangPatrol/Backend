package com.bbangpatrol.auth.service;

import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    // 로그인 관련
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        return null;
    }
}
