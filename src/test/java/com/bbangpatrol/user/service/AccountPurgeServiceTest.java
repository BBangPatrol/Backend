package com.bbangpatrol.user.service;

import com.bbangpatrol.user.repository.AccountPurgeRepository;
import com.bbangpatrol.user.service.AccountPurgeService.PurgeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 개인정보처리방침이 "탈퇴 후 30일 뒤 파기"를 약속했으므로, 이 작업이 멈추면 방침과 실제가 어긋난다.
 *
 * 특히 두 가지가 중요하다.
 *  - 지울 것이 없을 때 빈 IN 절로 쿼리가 나가면 SQL 이 깨진다.
 *  - FK 때문에 삭제 순서가 어긋나면 제약 위반으로 트랜잭션이 통째로 실패한다.
 */
@ExtendWith(MockitoExtension.class)
class AccountPurgeServiceTest {

    @Mock
    private AccountPurgeRepository purgeRepository;

    @InjectMocks
    private AccountPurgeService accountPurgeService;

    @Test
    @DisplayName("보관 기간이 지난 회원과 리뷰를 자식 테이블부터 지운다")
    void purgesInForeignKeySafeOrder() {
        when(purgeRepository.findExpiredUserIds(any())).thenReturn(List.of(7L));
        when(purgeRepository.findExpiredReviewIds(any())).thenReturn(List.of(100L));
        when(purgeRepository.findReviewIdsByUserIds(List.of(7L))).thenReturn(List.of(101L));
        when(purgeRepository.findReviewImageKeys(anyList())).thenReturn(List.of("reviews/a.jpg"));
        when(purgeRepository.findUserImageKeys(List.of(7L))).thenReturn(List.of("users/7.jpg"));

        PurgeResult result = accountPurgeService.purgeExpired(30);

        // 탈퇴 회원의 리뷰(101)도 삭제 표시가 없어도 함께 지운다
        assertThat(result.purgedUsers()).isEqualTo(1);
        assertThat(result.purgedReviews()).isEqualTo(2);
        assertThat(result.imageKeys()).containsExactly("reviews/a.jpg", "users/7.jpg");

        InOrder order = inOrder(purgeRepository);
        order.verify(purgeRepository).deleteReviewLikesByReviewIds(List.of(100L, 101L));
        order.verify(purgeRepository).deleteReviewImagesByReviewIds(List.of(100L, 101L));
        order.verify(purgeRepository).deleteReviewKeywordsByReviewIds(List.of(100L, 101L));
        order.verify(purgeRepository).deleteReviewsByIds(List.of(100L, 101L));
        order.verify(purgeRepository).deleteReviewLikesByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteVisitDetailsByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteVisitsByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteBookmarksByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteUserItemsByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteMissionProgressByUserIds(List.of(7L));
        order.verify(purgeRepository).deletePointHistoryByUserIds(List.of(7L));
        order.verify(purgeRepository).deleteUsersByIds(List.of(7L));
    }

    @Test
    @DisplayName("지울 것이 없으면 삭제 쿼리를 아예 보내지 않는다")
    void doesNothingWhenNothingExpired() {
        when(purgeRepository.findExpiredUserIds(any())).thenReturn(List.of());
        when(purgeRepository.findExpiredReviewIds(any())).thenReturn(List.of());

        PurgeResult result = accountPurgeService.purgeExpired(30);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.imageKeys()).isEmpty();

        // 빈 목록으로 IN 절을 만들면 SQL 이 깨진다
        verify(purgeRepository, never()).deleteUsersByIds(anyList());
        verify(purgeRepository, never()).deleteReviewsByIds(anyList());
        verify(purgeRepository).findExpiredUserIds(any());
        verify(purgeRepository).findExpiredReviewIds(any());
        verifyNoMoreInteractions(purgeRepository);
    }

    @Test
    @DisplayName("보관 기간만큼 지난 시점을 기준으로 찾는다")
    void usesRetentionDaysAsCutoff() {
        when(purgeRepository.findExpiredUserIds(any())).thenReturn(List.of());
        when(purgeRepository.findExpiredReviewIds(any())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(30);
        accountPurgeService.purgeExpired(30);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(purgeRepository).findExpiredUserIds(cutoff.capture());

        // 30일 전 근처여야 한다 (테스트 실행 시간만큼의 오차만 허용)
        assertThat(cutoff.getValue()).isBetween(before.minusMinutes(1), before.plusMinutes(1));
    }

    @Test
    @DisplayName("탈퇴 회원이 없으면 회원 관련 삭제는 건너뛴다")
    void skipsUserDeletesWhenOnlyReviewsExpired() {
        when(purgeRepository.findExpiredUserIds(any())).thenReturn(List.of());
        when(purgeRepository.findExpiredReviewIds(any())).thenReturn(List.of(100L));
        when(purgeRepository.findReviewImageKeys(List.of(100L))).thenReturn(List.of());

        PurgeResult result = accountPurgeService.purgeExpired(30);

        assertThat(result.purgedUsers()).isZero();
        assertThat(result.purgedReviews()).isEqualTo(1);
        verify(purgeRepository).deleteReviewsByIds(List.of(100L));
        verify(purgeRepository, never()).deleteUsersByIds(anyList());
        verify(purgeRepository, never()).findUserImageKeys(anyList());
    }
}
