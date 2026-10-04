package com.bbangpatrol.ocr.dto;

/**
 * 가게를 고르지 않고 영수증만 올렸을 때의 응답.
 * 기존 OcrResponse 에 "영수증으로 찾아낸 가게"(storeId, storeName)가 더해진 형태다.
 * 클라이언트는 storeId 로 방문 등록(POST /stores/{storeId}/visits)을 이어서 호출한다.
 */
public record ReceiptMatchResponse(
        Long storeId,
        String storeName,
        String bakeryName,
        String date,
        Integer amount,
        String menu,
        String verificationToken
) {
}
