package com.bbangpatrol.ocr.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptMatchResponse;
import com.bbangpatrol.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    // 하나밖에 없으니 그냥 바로 받자
    @PostMapping(
            value = "/api/v1/stores/{storeId}/visit-verifications",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<ApiResponse<OcrResponse>> getReceiptInfo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestPart("receipt") MultipartFile receipt) {

        OcrResponse response = ocrService.getInfo(userId, storeId, receipt);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "요청이 성공적입니다.",
                        response));
    }

    /**
     * 가게를 고르지 않고 영수증만 올리는 인증.
     * 영수증에서 가게를 찾아 응답에 담아 주고, 방문 등록은 기존 API(/stores/{storeId}/visits)를 그대로 쓴다.
     * 가게를 특정하지 못하면 가게를 고르는 기존 흐름으로 보내면 된다 (409).
     */
    @PostMapping(
            value = "/api/v1/visit-verifications",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<ApiResponse<ReceiptMatchResponse>> getReceiptInfoWithoutStore(
            @AuthenticationPrincipal Long userId,
            @RequestPart("receipt") MultipartFile receipt) {

        ReceiptMatchResponse response = ocrService.getInfoByReceipt(userId, receipt);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.onSuccess(
                        HttpStatus.OK,
                        "요청이 성공적입니다.",
                        response));
    }
}
