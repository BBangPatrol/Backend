package com.bbangpatrol.user.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.user.dto.UserRequestDTO;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.service.AccountWithdrawService;
import com.bbangpatrol.user.service.UserService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;
    private final AccountWithdrawService accountWithdrawService;

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

    /**
     * 회원 탈퇴. 리뷰는 화면에서 즉시 내려가고, 계정 정보는 보관 기간(30일)이 지나면 파기된다.
     * 로그아웃과 같은 방식으로 Authorization 헤더의 액세스 토큰을 받아 함께 무효화한다.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        String bearerToken = request.getHeader("Authorization");
        String accessToken = (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;

        accountWithdrawService.withdraw(userId, accessToken);

        // 로그아웃과 같은 조건으로 지워야 브라우저가 쿠키를 실제로 제거한다
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(HttpStatus.OK, "회원 탈퇴가 완료되었습니다.", null));
    }
}
