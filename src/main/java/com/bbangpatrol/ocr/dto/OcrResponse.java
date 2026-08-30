package com.bbangpatrol.ocr.dto;

public record OcrResponse(
        String bakeryName,
        String date,
        Integer amount,
        String menu,
        String verificationToken
) {
}
