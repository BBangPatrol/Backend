package com.bbangpatrol.receiptVerification.service;

import com.bbangpatrol.ocr.service.ReceiptTokenService;
import com.bbangpatrol.receiptVerification.dto.VerificationRequest;
import com.bbangpatrol.receiptVerification.dto.VerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final ReceiptTokenService receiptTokenService;

    @Override
    public VerificationResponse doVerification(Long userId, Long storeId, VerificationRequest request) {
        log.info("[Verification Service] 사용자 영수증 인증 처리 시작. userId: {}, storeId: {}", userId, storeId);

        // 토큰 전달해서 사용자 영수증 승인번호 가져오기
        String receiptNum = receiptTokenService.consumeToken(request.getVerificationToken());

        // DB 중복 조회해서 없으면 인증 처리. 근데 OCR 단계에서도 승인번호 통해서 중복 확인 해봐야 할듯????


        return null;
    }

}
