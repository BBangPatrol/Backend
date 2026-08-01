package com.bbangpatrol.user.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    @GetMapping()
    public ApiResponse<UserResponseDTO.MyPageDTO> getMyPage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(userService.getMyPage(userId));
    }
}
