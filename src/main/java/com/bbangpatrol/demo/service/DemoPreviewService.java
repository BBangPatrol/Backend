package com.bbangpatrol.demo.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.demo.dto.PreviewReceiptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 시연용 영수증 사진을 코드로 열어 준다.
 *
 * 실물 영수증 없이 방문 인증 흐름을 보여줘야 하는데, 사진 URL 을 그냥 공개해 두면
 * 아무나 같은 영수증으로 인증을 시도할 수 있다. 그래서 로그인 + 코드 두 단계를 요구한다.
 * (로그인 강제는 SecurityConfig 의 anyRequest().authenticated() 가 맡는다.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DemoPreviewService {

    private final R2Service r2Service;

    @Value("${demo.preview.code}")
    private String previewCode;

    @Value("${demo.preview.receipt-key}")
    private String receiptKey;

    public PreviewReceiptResponse getPreviewReceipt(Long userId, String code) {
        // 시연 중 폰 키보드로 치는 코드라 앞뒤 공백과 대소문자는 눈감아 준다
        String entered = code == null ? "" : code.trim();

        if (!previewCode.equalsIgnoreCase(entered)) {
            log.warn("시연 코드 불일치. userId={}", userId);
            throw new ApiException(ErrorCode.INVALID_DEMO_CODE);
        }

        log.info("시연용 영수증 사진 발급. userId={}, key={}", userId, receiptKey);
        return new PreviewReceiptResponse(r2Service.getPublicUrl(receiptKey), fileName(receiptKey));
    }

    private static String fileName(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }
}
