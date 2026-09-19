package com.bbangpatrol.visit.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.visit.event.ReceiptVerifiedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final ReceiptTokenService receiptTokenService;
    private final ReceiptHashService receiptHashService;
    private final ReceiptDuplicateChecker receiptDuplicateChecker;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    private final BakeryRepository bakeryRepository;
    private final UserRepository userRepository;

    private final VisitDetailRepository visitDetailRepository;
    private final VisitRepository visitRepository;

    @Transactional
    @Override
    public VisitResponse doVerification(Long userId, Long storeId, VisitRequest request) {
        log.info("[Verification Service] 사용자 영수증 인증 처리 시작. userId: {}, storeId: {}", userId, storeId);

        // 토큰을 소모해서 1단계가 영수증에서 읽은 값을 가져온다.
        // 발급받은 사용자·가게가 아니면 여기서 막힌다.
        ReceiptTokenService.ReceiptTicket ticket =
                receiptTokenService.consumeToken(request.getVerificationToken(), userId, storeId);
        String receiptNum = ticket.receiptNum();
        log.info("[Verification Service] 토큰을 통해 조회한 사용자의 영수증 승인번호 조회 {}", receiptNum);

        // 저장·지급에 쓰는 값은 요청 본문이 아니라 토큰에 담긴 값이다.
        // 본문 값을 쓰면 금액을 바꿔 보내는 것만으로 포인트를 늘리고 중복 방지 해시를 피할 수 있다.
        // (옛 토큰에는 금액·날짜가 없어 그때는 예전처럼 본문 값으로 돌아간다.)
        int totalAmount = ticket.amount() != null ? ticket.amount() : request.getTotalAmount();
        LocalDate visitedAt = ticket.date() != null ? ticket.date() : request.getDate();

        if (ticket.amount() != null && !ticket.amount().equals(request.getTotalAmount())) {
            log.warn("[Verification Service] 요청 본문 금액이 영수증과 다르다. 영수증={}, 본문={}",
                    ticket.amount(), request.getTotalAmount());
        }
        if (ticket.date() != null && !ticket.date().equals(request.getDate())) {
            log.warn("[Verification Service] 요청 본문 날짜가 영수증과 다르다. 영수증={}, 본문={}",
                    ticket.date(), request.getDate());
        }

        // 2. 빵집 조회
        Bakery bakery = bakeryRepository.findByIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.BAKERY_NOT_FOUND)
                );

        // 3. 사용자 조회. 같은 유저의 동시 인증을 여기서 줄 세운다.
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 4. 사업자번호 + 승인번호 + 날짜 + 금액으로 영수증 해시 생성.
        //    번호는 OCR 단계가 영수증에서 읽어 토큰에 실어 보낸 값을 쓴다. 가게에 저장된 번호를 쓰면
        //    사업자번호를 모르는 가게에서 OCR 단계의 중복 검사와 다른 해시가 나온다.
        //    (옛 토큰은 번호가 없으므로 예전처럼 가게 값으로 돌아간다)
        String receiptHash = receiptHashService.create(
                ticket.businessNumber() != null ? ticket.businessNumber() : bakery.getBusinessNumber(),
                receiptNum,
                visitedAt,
                totalAmount
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
                .totalAmount(totalAmount)
                .visitedAt(visitedAt)
                .receiptHash(receiptHash)
                .createdAt(LocalDateTime.now())
                .visit(visit)
                .build();

        visitDetailRepository.save(visitDetail);

        // 총 금액으로 포인트 계산. 1000원당 100포인트라서 1000으로 나눈 후에 100 곱하기
        int point = (totalAmount / 1000) * 100;

        // 1000원 미만이면 0포인트다. 적립 내역에 0원 행을 남기지 않는다
        if (point > 0) {
            pointService.updatePoint(userId, point, true, "영수증 인증");
        }

        // 영수증 / 빵집 방문 미션 진행도 갱신은 커밋 뒤에 한다.
        // 같은 트랜잭션에서 돌리면 미션 쪽 오류가 인증 자체를 롤백시킨다 (MissionEventListener 참고)
        eventPublisher.publishEvent(new ReceiptVerifiedEvent(userId, bakery.getRegion()));

        return new VisitResponse(
                visit.getId(),
                point
        );
    }

}
