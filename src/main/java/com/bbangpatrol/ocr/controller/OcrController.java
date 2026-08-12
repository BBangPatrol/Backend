package com.bbangpatrol.ocr.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    // 하나밖에 없으니 그냥 바로 받자
    @PostMapping("/api/v1/stores/{storeId}/visit-verifications")
    ResponseEntity<ApiResponse<OcrResponse>> getReceiptInfo(
            @AuthenticationPrincipal Long userId,
            @PathVariable String storeId) {

        OcrResponse data = ocrService.getInfo(userId, storeId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "요청이 성공적입니다.",
                        data));
    }
}
