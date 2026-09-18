package com.bbangpatrol.visit.service;

import com.bbangpatrol.visit.repository.VisitDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 영수증 재사용 판정. OCR 단계와 인증 확정 단계가 같은 기준을 써야
 * 한쪽만 통과하고 다른 쪽에서 막히는 일이 없다.
 * <p>
 * 기본값 global 은 영수증 한 장을 전체에서 한 번만 쓰게 한다.
 * 시연처럼 한 장을 여러 사람에게 돌려야 하면 user 로 바꾼다.
 * 그때도 같은 사람이 두 번 쓰는 건 막힌다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptDuplicateChecker {

    private static final String SCOPE_USER = "user";

    private final VisitDetailRepository visitDetailRepository;

    @Value("${receipt.duplicate-scope:global}")
    private String scope;

    public boolean isAlreadyUsed(String receiptHash, Long userId) {
        if (SCOPE_USER.equalsIgnoreCase(scope)) {
            log.debug("[RECEIPT] 영수증 중복 검사 범위가 user 다. 사용자별로만 막는다. userId: {}", userId);
            return visitDetailRepository.existsByReceiptHashAndUserId(receiptHash, userId);
        }

        return visitDetailRepository.existsByReceiptHash(receiptHash);
    }
}
