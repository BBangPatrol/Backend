package com.bbangpatrol.visit.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.visit.event.ReceiptVerifiedEvent;
import com.bbangpatrol.ocr.service.ReceiptTokenService;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.dto.VisitRequest;
import com.bbangpatrol.visit.dto.VisitResponse;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.entity.VisitDetail;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import com.bbangpatrol.visit.repository.VisitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 영수증 인증은 포인트가 걸린 입구다. 같은 영수증을 두 번 인정하면 포인트를 무한히 찍을 수 있고,
 * 방문 기록이 잘못 쌓이면 리뷰 작성 자격까지 새어 나간다.
 */
@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    private static final long USER_ID = 6L;
    private static final long STORE_ID = 101L;
    private static final String TOKEN = "verification-token";
    private static final String RECEIPT_NUM = "0001";
    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String HASH = "hashed-receipt";

    @Mock
    private ReceiptTokenService receiptTokenService;
    @Mock
    private ReceiptHashService receiptHashService;
    @Mock
    private ReceiptDuplicateChecker receiptDuplicateChecker;
    @Mock
    private PointService pointService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VisitDetailRepository visitDetailRepository;
    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Test
    @DisplayName("이미 쓴 영수증이면 막고 포인트도 방문 기록도 남기지 않는다")
    void rejectsAlreadyUsedReceipt() {
        givenTokenAndBakery(12000);
        when(receiptDuplicateChecker.isAlreadyUsed(HASH, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> verificationService.doVerification(USER_ID, STORE_ID, request(12000)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_ALREADY_USED);

        // 하나라도 새면 같은 영수증으로 포인트를 반복해서 벌 수 있다
        verify(visitDetailRepository, never()).save(any());
        verify(pointService, never()).updatePoint(anyLong(), anyInt(), anyBoolean(), any());
        // 인증이 막혔으면 미션 갱신 신호도 나가면 안 된다
        verify(eventPublisher, never()).publishEvent(any(ReceiptVerifiedEvent.class));
    }

    @Test
    @DisplayName("정상 인증이면 방문 횟수가 오르고 방문 상세가 저장된다")
    void savesVisitDetailAndBumpsCount() {
        Visit visit = givenTokenBakeryAndVisit(12000);

        VisitResponse response = verificationService.doVerification(USER_ID, STORE_ID, request(12000));

        assertThat(visit.getCount()).isEqualTo(1);
        assertThat(response.getVisitId()).isEqualTo(visit.getId());

        ArgumentCaptor<VisitDetail> captor = ArgumentCaptor.forClass(VisitDetail.class);
        verify(visitDetailRepository).save(captor.capture());
        VisitDetail saved = captor.getValue();
        assertThat(saved.getTotalAmount()).isEqualTo(12000);
        assertThat(saved.getVisitedAt()).isEqualTo(LocalDate.of(2026, 9, 5));
        // 이 해시가 다음번 중복 판정의 근거가 된다
        assertThat(saved.getReceiptHash()).isEqualTo(HASH);
        assertThat(saved.getVisit()).isSameAs(visit);
    }

    @Test
    @DisplayName("포인트는 1000원당 100점이고 1000원 미만은 버린다")
    void awardsHundredPointsPerThousandWon() {
        givenTokenBakeryAndVisit(12900);

        VisitResponse response = verificationService.doVerification(USER_ID, STORE_ID, request(12900));

        // 12900 / 1000 = 12 -> 1200
        assertThat(response.getPoint()).isEqualTo(1200);
        verify(pointService).updatePoint(USER_ID, 1200, true, "영수증 인증");
    }

    @Test
    @DisplayName("1000원 미만 영수증은 0포인트로 처리된다")
    void awardsNothingBelowOneThousandWon() {
        givenTokenBakeryAndVisit(900);

        VisitResponse response = verificationService.doVerification(USER_ID, STORE_ID, request(900));

        assertThat(response.getPoint()).isZero();
        // 0원 적립 내역이 쌓이면 포인트 내역 화면이 지저분해진다
        verify(pointService, never()).updatePoint(anyLong(), anyInt(), anyBoolean(), any());
    }

    @Test
    @DisplayName("해시는 가게 사업자번호와 영수증 정보를 함께 넣어 만든다")
    void hashesBusinessNumberWithReceiptFields() {
        givenTokenBakeryAndVisit(12000);

        verificationService.doVerification(USER_ID, STORE_ID, request(12000));

        verify(receiptHashService).create("123-45-67890", RECEIPT_NUM, LocalDate.of(2026, 9, 5), 12000);
    }

    @Test
    @DisplayName("저장 금액과 날짜는 요청 본문이 아니라 영수증에서 읽은 값이다")
    void usesTicketValuesOverRequestBody() {
        givenTokenBakeryAndVisit(12000);

        // 본문으로 큰 금액과 엉뚱한 날짜를 보내도 무시돼야 한다.
        // 그러지 않으면 금액만 바꿔 보내 포인트를 늘리고 중복 방지 해시를 매번 피할 수 있다.
        VisitResponse response = verificationService.doVerification(
                USER_ID, STORE_ID, new VisitRequest(99_999_000, LocalDate.of(2030, 1, 1), TOKEN));

        ArgumentCaptor<VisitDetail> captor = ArgumentCaptor.forClass(VisitDetail.class);
        verify(visitDetailRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualTo(12000);
        assertThat(captor.getValue().getVisitedAt()).isEqualTo(LocalDate.of(2026, 9, 5));

        assertThat(response.getPoint()).isEqualTo(1200);
        verify(pointService).updatePoint(USER_ID, 1200, true, "영수증 인증");
        verify(receiptHashService).create(BUSINESS_NUMBER, RECEIPT_NUM, LocalDate.of(2026, 9, 5), 12000);
    }

    @Test
    @DisplayName("없는 빵집이면 토큰만 쓰고 막힌다")
    void rejectsUnknownBakery() {
        when(receiptTokenService.consumeToken(TOKEN, USER_ID, STORE_ID)).thenReturn(ticket(12000));
        when(bakeryRepository.findByIdAndDeletedAtIsNull(STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.doVerification(USER_ID, STORE_ID, request(12000)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAKERY_NOT_FOUND);

        verify(visitDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증이 끝나면 미션 갱신 신호를 커밋 뒤로 넘긴다")
    void publishesMissionEventAfterVerification() {
        givenTokenBakeryAndVisit(12000);

        verificationService.doVerification(USER_ID, STORE_ID, request(12000));

        // 미션 갱신을 같은 트랜잭션에서 하면 미션 오류가 인증까지 롤백시킨다
        ArgumentCaptor<ReceiptVerifiedEvent> captor = ArgumentCaptor.forClass(ReceiptVerifiedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
    }

    private VisitRequest request(int totalAmount) {
        return new VisitRequest(totalAmount, LocalDate.of(2026, 9, 5), TOKEN);
    }

    private ReceiptTokenService.ReceiptTicket ticket(int amount) {
        return new ReceiptTokenService.ReceiptTicket(
                USER_ID, STORE_ID, RECEIPT_NUM, BUSINESS_NUMBER, amount, LocalDate.of(2026, 9, 5));
    }

    private void givenTokenAndBakery(int amount) {
        when(receiptTokenService.consumeToken(TOKEN, USER_ID, STORE_ID)).thenReturn(ticket(amount));
        when(bakeryRepository.findByIdAndDeletedAtIsNull(STORE_ID)).thenReturn(Optional.of(bakery()));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(receiptHashService.create(any(), any(), any(), any())).thenReturn(HASH);
    }

    private Visit givenTokenBakeryAndVisit(int amount) {
        givenTokenAndBakery(amount);
        lenient().when(receiptDuplicateChecker.isAlreadyUsed(HASH, USER_ID)).thenReturn(false);

        Visit visit = Visit.create(user(), bakery());
        ReflectionId.set(visit, 77L);
        when(visitRepository.findForUpdate(eq(USER_ID), eq(STORE_ID))).thenReturn(Optional.of(visit));
        return visit;
    }

    private User user() {
        return User.builder().id(USER_ID).name("빵순이").pointBalance(0).build();
    }

    private Bakery bakery() {
        return Bakery.builder()
                .id(STORE_ID)
                .name("성심당")
                .region(Region.JUNG)
                .businessNumber("123-45-67890")
                .build();
    }

    /** Visit.create 는 id 를 세팅하지 않는다. 응답의 visitId 를 확인하려면 채워 넣어야 한다. */
    private static final class ReflectionId {
        static void set(Object target, Long id) {
            try {
                var field = target.getClass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
