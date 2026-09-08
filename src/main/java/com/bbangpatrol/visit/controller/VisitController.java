package com.bbangpatrol.visit.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.visit.dto.VisitRequest;
import com.bbangpatrol.visit.dto.VisitResponse;
import com.bbangpatrol.visit.service.VerificationService;
import jakarta.validation.Valid;
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
public class VisitController {

    private final VerificationService verificationService;

    @PostMapping("/api/v1/stores/{storeId}/visits")
    ResponseEntity<ApiResponse<VisitResponse>> receiptVerification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestBody @Valid VisitRequest request) {

        VisitResponse response = verificationService.doVerification(userId, storeId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(
                        HttpStatus.CREATED,
                        "요청이 성공적입니다.",
                        response
                ));
    }
}
