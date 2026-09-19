package com.bbangpatrol.ocr.service;

import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptMatchResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    // 영수증에서 정보를 추출하기 위한 메서드 (사용자가 가게를 고른 흐름)
    OcrResponse getInfo(Long userId, Long storeId, MultipartFile receipt);

    // 가게를 고르지 않고 영수증만 올리는 흐름. 영수증에서 가게를 찾아낸다
    ReceiptMatchResponse getInfoByReceipt(Long userId, MultipartFile receipt);
}
