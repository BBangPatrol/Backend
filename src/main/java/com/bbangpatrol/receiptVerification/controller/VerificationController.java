package com.bbangpatrol.receiptVerification.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.receiptVerification.dto.VerificationRequest;
import com.bbangpatrol.receiptVerification.dto.VerificationResponse;
import com.bbangpatrol.receiptVerification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/api/v1/stores/{storeId}/visits")
    ResponseEntity<ApiResponse<VerificationResponse>> receiptVerification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestBody VerificationRequest request) {

        VerificationResponse response = verificationService.doVerification(userId, storeId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "요청이 성공적입니다.",
                        response
                ));
    }
}
