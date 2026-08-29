package com.bbangpatrol.bakery.controller;

import com.bbangpatrol.bakery.dto.*;
import com.bbangpatrol.bakery.service.BakeryService;
import com.bbangpatrol.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class BakeryController {

    private final BakeryService bakeryService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<BakerySearchResponse>> searchBakeries(@AuthenticationPrincipal Long userId, @ModelAttribute BakerySearchRequest request) {
        BakerySearchResponse response = bakeryService.searchBakeries(userId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(HttpStatus.OK, response));
    }

    @PostMapping("/{storeId}/favorites")
    public ResponseEntity<ApiResponse<BakeryFavoriteResponse>> toggleFavorite(@AuthenticationPrincipal Long userId, @PathVariable long storeId) {
        BakeryFavoriteResponse response = bakeryService.toggleFavorite(userId, storeId);
        HttpStatus status = response.likes() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(ApiResponse.onSuccess(status, response));
    }

    @GetMapping("/{storeId}/detail")
    public ResponseEntity<ApiResponse<BakeryDetailResponse>> getBakeryDetail(@AuthenticationPrincipal Long userId, @PathVariable long storeId) {
        BakeryDetailResponse response = bakeryService.getBakeryDetail(userId, storeId);
        return ResponseEntity.ok(ApiResponse.onSuccess(HttpStatus.OK, response));
    }

    @GetMapping("/{storeId}/attractions")
    public ResponseEntity<ApiResponse<AttractionListResponse>> getNearbyAttractions(
            @PathVariable long storeId
    ) {
        AttractionListResponse response = bakeryService.getNearbyAttractions(storeId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.onSuccess(
                HttpStatus.OK, "요청이 성공적입니다.", response));
    }
}
