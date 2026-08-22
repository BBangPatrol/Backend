package com.bbangpatrol.ocr.service;

import com.bbangpatrol.ocr.dto.OcrResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    // 영수증에서 정보를 추출하기 위한 메서드
    OcrResponse getInfo(Long userId, Long storeId, MultipartFile receipt);
}
