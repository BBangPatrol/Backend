package com.bbangpatrol.auth.controller;

import com.bbangpatrol.auth.dto.LoginRequest;
import com.bbangpatrol.auth.dto.LoginResponse;
import com.bbangpatrol.auth.service.AuthService;
import com.bbangpatrol.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest loginRequest) {

        LoginResponse response = authService.login(loginRequest);

        if(response.isNewUser()) { // 신규 유저일 경우에는 201 리턴
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.onSuccess(
                            HttpStatus.CREATED,
                            "회원가입에 성공했습니다.",
                            response
                    ));
        }
        return ResponseEntity.status(HttpStatus.OK) // 기존 유저일 경우에는 200 리턴
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "로그인에 성공했습니다.",
                        response
                ));
    }
}
