package com.bbangpatrol.user.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.user.dto.UserRequestDTO;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    @GetMapping()
    public ApiResponse<UserResponseDTO.MyPageDTO> getMyPage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(userService.getMyPage(userId));
    }

    @PatchMapping("/edit")
    public ResponseEntity<ApiResponse> editNickname(@AuthenticationPrincipal Long userId, @RequestBody @Valid UserRequestDTO.EditNicknameDTO request) {
        userService.editNickname(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile-image")
    public ApiResponse<UserResponseDTO.ProfileImageDTO> getMyProfileImage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(userService.getMyProfileImage(userId));
    }

    @PostMapping("/profile-image")
    public ResponseEntity<ApiResponse> postProfileImage(@AuthenticationPrincipal Long userId, @RequestPart("profileImage") MultipartFile profileImage) throws IOException {
        userService.postProfileImage(userId, profileImage);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/points")
    public ApiResponse<UserResponseDTO.PointHistoryDTO> getPointsHistory(@AuthenticationPrincipal Long userId, @RequestParam(required = false, defaultValue = "0") int page) {
        UserResponseDTO.PointHistoryDTO data = userService.getPointHistory(userId, page);
        return ApiResponse.onSuccess(data);
    }

    @GetMapping("/reviews")
    public ApiResponse<UserResponseDTO.ReviewHistoryDTO> getMyReviews(@AuthenticationPrincipal Long userId, @RequestParam(required = false, defaultValue = "0") int page) {
        return ApiResponse.onSuccess(userService.getMyReviews(userId, page));
    }

    @GetMapping("/bread-collections")
    public ApiResponse<UserResponseDTO.VisitedBakeryListDTO> getBakeryList(@AuthenticationPrincipal Long userId, @RequestParam(required = false, defaultValue = "") String query) {
        return ApiResponse.onSuccess(userService.getBakeryList(userId, query));
    }
}
