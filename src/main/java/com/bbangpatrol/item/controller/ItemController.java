package com.bbangpatrol.item.controller;

import com.bbangpatrol.common.util.ApiResponse;
import com.bbangpatrol.item.dto.DrawResult;
import com.bbangpatrol.item.dto.DrawResultResponse;
import com.bbangpatrol.item.dto.ItemListResponse;
import com.bbangpatrol.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collectibles")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<ApiResponse<ItemListResponse>> getItems(
//            @AuthenticationPrincipal Long userId,
            @RequestParam Long userId,
            @RequestParam String type
    ) {
        ItemListResponse itemListResponse = itemService.getItems(userId, type);
        return ResponseEntity.ok(ApiResponse.onSuccess(itemListResponse));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DrawResultResponse>> drawItem(
            @AuthenticationPrincipal Long userId
    ) {
        DrawResult drawResult = itemService.drawItem(userId);
        HttpStatus status = drawResult.isDuplicated() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = drawResult.isDuplicated() ? "요청은 성공적이나, 중복 아이템이 뽑혀 소량의 포인트를 환불해드립니다"
                : "뽑기에 성공했습니다.";
        return ResponseEntity.ok(ApiResponse.onSuccess(status, message, drawResult.response()));
    }
}
