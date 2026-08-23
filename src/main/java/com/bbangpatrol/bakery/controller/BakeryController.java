package com.bbangpatrol.bakery.controller;

import com.bbangpatrol.bakery.dto.AttractionListResponse;
import com.bbangpatrol.bakery.service.BakeryService;
import com.bbangpatrol.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class BakeryController {

    private final BakeryService bakeryService;

    @GetMapping("/{storeId}/attractions")
    public ResponseEntity<ApiResponse<AttractionListResponse>> getNearbyAttractions(
            @PathVariable long storeId
    ) {
        AttractionListResponse response = bakeryService.getNearbyAttractions(storeId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.onSuccess(
                HttpStatus.OK, "요청이 성공적입니다.", response));
    }
}
