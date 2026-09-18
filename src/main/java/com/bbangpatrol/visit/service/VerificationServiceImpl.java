package com.bbangpatrol.visit.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.mission.service.MissionEvaluator;
import com.bbangpatrol.ocr.dto.ReceiptTokenPayload;
import com.bbangpatrol.ocr.service.ReceiptTokenService;
import com.bbangpatrol.point.service.PointService;
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
    private final ReceiptDuplicateChecker receiptDuplicateChecker;
    private final PointService pointService;
    private final MissionEvaluator missionEvaluator;

    private final BakeryRepository bakeryRepository;
    private final UserRepository userRepository;

    private final VisitDetailRepository visitDetailRepository;
    private final VisitRepository visitRepository;

    @Transactional
    @Override
    public VisitResponse doVerification(Long userId, Long storeId, VisitRequest request) {
        log.info("[Verification Service] 사용자 영수증 인증 처리 시작. userId: {}, storeId: {}", userId, storeId);

        // 토큰 전달해서 OCR 단계에서 확정한 승인번호 / 사업자번호 / 지역 가져오기
        ReceiptTokenPayload payload = receiptTokenService.consumeToken(request.getVerificationToken());
        log.info("[Verification Service] 토큰을 통해 조회한 사용자의 영수증 승인번호 조회 {}", payload.receiptNum());

        // 2. 빵집 조회
        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.BAKERY_NOT_FOUND)
                );

        // 3. 사용자 조회. 같은 유저의 동시 인증을 여기서 줄 세운다.
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 4. 사업자번호 + 승인번호 + 날짜 + 금액으로 영수증 해시 생성.
        //    사업자번호는 OCR 단계에서 매칭한 영수증의 실제 번호라야 지점별로 해시가 갈린다
        String receiptHash = receiptHashService.create(
                payload.businessNumber(),
                payload.receiptNum(),
                request.getDate(),
                request.getTotalAmount()
        );

        // DB 중복 조회해서 없으면 인증 처리. 시연용으로 열어두면 사용자별로만 막는다
        if (receiptDuplicateChecker.isAlreadyUsed(receiptHash, userId)) {
            throw new ApiException(ErrorCode.RECEIPT_ALREADY_USED);
        }

        // 6. 방문 기록 확보. 처음 방문이면 이때 만들어진다
        visitRepository.insertIfAbsent(userId, storeId);
        Visit visit = visitRepository.findForUpdate(userId, storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        // 7. 방문 횟수 +1
        visit.increaseCount();

        // 8. 개별 방문 기록 저장
        VisitDetail visitDetail = VisitDetail.builder()
                .totalAmount(request.getTotalAmount())
                .visitedAt(request.getDate())
                .receiptHash(receiptHash)
                // 구별 미션은 지도에 실린 구가 아니라 여기 적힌 구로 집계된다
                .region(payload.region())
                .receiptBusinessNumber(payload.businessNumber())
                .createdAt(LocalDateTime.now())
                .visit(visit)
                .build();

        visitDetailRepository.save(visitDetail);

        // 총 금액으로 포인트 계산. 1000원당 100포인트라서 1000으로 나눈 후에 100 곱하기
        int point = (request.getTotalAmount() / 1000) * 100;

        // 포인트 적립
        pointService.updatePoint(userId, point, true);

        // 영수증 / 빵집 방문 미션 진행도 갱신.
        // 지점 영수증이면 빵집이 실린 구가 아니라 실제로 방문한 구로 집계한다
        missionEvaluator.onReceiptVerified(userId, payload.region());

        return new VisitResponse(
                visit.getId(),
                point
        );
    }

}
