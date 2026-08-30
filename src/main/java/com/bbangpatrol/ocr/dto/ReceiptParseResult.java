package com.bbangpatrol.ocr.dto;

public record ReceiptParseResult(
        String bakeryName,
        String date,
        Integer amount,
        String menu,
        String receiptNum,
        String businessNumber
) {
}