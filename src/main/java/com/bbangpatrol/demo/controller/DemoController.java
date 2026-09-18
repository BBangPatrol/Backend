package com.bbangpatrol.demo.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.demo.dto.PreviewReceiptRequest;
import com.bbangpatrol.demo.dto.PreviewReceiptResponse;
import com.bbangpatrol.demo.service.DemoPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoPreviewService demoPreviewService;

    /** 코드를 맞히면 시연용 영수증 사진 URL 을 준다. 로그인 필수 (SecurityConfig 기본 정책). */
    @PostMapping("/preview-receipt")
    ResponseEntity<ApiResponse<PreviewReceiptResponse>> getPreviewReceipt(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PreviewReceiptRequest request) {

        PreviewReceiptResponse response = demoPreviewService.getPreviewReceipt(userId, request.code());

        return ResponseEntity.ok(ApiResponse.onSuccess(HttpStatus.OK, "요청이 성공적입니다.", response));
    }
}
