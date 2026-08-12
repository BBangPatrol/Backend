package com.bbangpatrol.ocr.service;

import com.bbangpatrol.ocr.dto.OcrResponse;

public interface OcrService {
    OcrResponse getInfo(Long userId, String storeId);
}
