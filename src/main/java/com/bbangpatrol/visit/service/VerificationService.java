package com.bbangpatrol.visit.service;

import com.bbangpatrol.visit.dto.VisitRequest;
import com.bbangpatrol.visit.dto.VisitResponse;

public interface VerificationService {
    VisitResponse doVerification(Long userId, Long storeId, VisitRequest request);
}
