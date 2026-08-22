package com.bbangpatrol.receiptVerification.service;

import com.bbangpatrol.receiptVerification.dto.VerificationRequest;
import com.bbangpatrol.receiptVerification.dto.VerificationResponse;

public interface VerificationService {
    VerificationResponse doVerification(Long userId, Long storeId, VerificationRequest request);
}
