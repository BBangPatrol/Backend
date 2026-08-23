package com.bbangpatrol.visit.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.service.ReceiptTokenService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.dto.VisitRequest;
import com.bbangpatrol.visit.dto.VisitResponse;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.entity.VisitDetail;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import com.bbangpatrol.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final ReceiptTokenService receiptTokenService;
    private final ReceiptHashService receiptHashService;

    private final BakeryRepository bakeryRepository;
    private final UserRepository userRepository;

    private final VisitDetailRepository visitDetailRepository;
    private final VisitRepository visitRepository;

    @Transactional
    @Override
    public VisitResponse doVerification(Long userId, Long storeId, VisitRequest request) {
        log.info("[Verification Service] 사용자 영수증 인증 처리 시작. userId: {}, storeId: {}", userId, storeId);

        // 토큰 전달해서 사용자 영수증 승인번호 가져오기
        String receiptNum = receiptTokenService.consumeToken(request.getVerificationToken());
        log.info("[Verification Service] 토큰을 통해 조회한 사용자의 영수증 승인번호 조회 {}", receiptNum);

        // 2. 빵집 조회
        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.BAKERY_NOT_FOUND)
                );

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.USER_NOT_FOUND)
                );

        // 4. 사업자번호 + 승인번호 + 날짜 + 금액으로 영수증 해시 생성
        String receiptHash = receiptHashService.create(
                bakery.getBusinessNumber(),
                receiptNum,
                request.getDate(),
                request.getTotalAmount()
        );

        // DB 중복 조회해서 없으면 인증 처리. 근데 OCR 단계에서도 승인번호 통해서 중복 확인 해봐야 할듯????
        if (visitDetailRepository.existsByReceiptHash(receiptHash)) {
            throw new ApiException(ErrorCode.RECEIPT_ALREADY_USED);
        }

        Visit visit = visitRepository
                .findByUserIdAndBakeryId(userId, storeId)
                .orElseGet(() -> Visit.create(user, bakery));

        // 7. 방문 횟수 +1
        visit.increaseCount();

        if (visit.getId() == null) { // 만약 조회된 게 없다면 처음 방문한 것이므로 내용 저장
            visitRepository.save(visit);
        }

        // 8. 개별 방문 기록 저장
        VisitDetail visitDetail = VisitDetail.builder()
                .totalAmount(request.getTotalAmount())
                .visitedAt(request.getDate())
                .receiptHash(receiptHash)
                .createdAt(LocalDateTime.now())
                .visit(visit)
                .build();

        visitDetailRepository.save(visitDetail);

        // 총 금액으로 포인트 계산. 1000원당 100포인트라서 1000으로 나눈 후에 100 곱하기
        int point = (request.getTotalAmount() / 1000) * 100;

        return new VisitResponse(
                visit.getId(),
                point
        );
    }

}
