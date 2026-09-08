package com.bbangpatrol.point.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.point.entity.Point;
import com.bbangpatrol.point.entity.PointType;
import com.bbangpatrol.point.repository.PointRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 포인트는 영수증 인증으로 벌고 뽑기로 쓴다. 잔액과 이력이 어긋나면 사용자가 바로 알아채는 자리라
 * 잔액 변화와 남는 이력 행을 함께 본다.
 */
@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    private static final long USER_ID = 6L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointServiceImpl pointService;

    @Test
    @DisplayName("적립하면 잔액이 늘고 earn 이력이 남는다")
    void earnIncreasesBalanceAndLeavesHistory() {
        User user = user(500);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        pointService.updatePoint(USER_ID, 1200, true);

        assertThat(user.getPointBalance()).isEqualTo(1700);
        Point history = savedHistory();
        assertThat(history.getType()).isEqualTo(PointType.earn);
        assertThat(history.getAmount()).isEqualTo(1200);
        assertThat(history.getContent()).isEqualTo("포인트 적립");
        assertThat(history.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("사용하면 잔액이 줄고 이력에는 음수로 남는다")
    void spendDecreasesBalanceAndRecordsNegativeAmount() {
        User user = user(500);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        pointService.updatePoint(USER_ID, 100, false);

        assertThat(user.getPointBalance()).isEqualTo(400);
        Point history = savedHistory();
        assertThat(history.getType()).isEqualTo(PointType.spend);
        // 이력을 합산하면 잔액이 나와야 하므로 사용은 음수로 적힌다
        assertThat(history.getAmount()).isEqualTo(-100);
        assertThat(history.getContent()).isEqualTo("포인트 사용");
    }

    @Test
    @DisplayName("사유를 넘기면 그대로 이력에 적힌다")
    void keepsGivenContent() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(500)));

        pointService.updatePoint(USER_ID, 100, false, "수집품 뽑기");

        assertThat(savedHistory().getContent()).isEqualTo("수집품 뽑기");
    }

    @Test
    @DisplayName("잔액보다 많이 쓰려 하면 막고 이력도 남기지 않는다")
    void rejectsSpendingMoreThanBalance() {
        User user = user(50);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> pointService.updatePoint(USER_ID, 100, false))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

        assertThat(user.getPointBalance()).isEqualTo(50);
        // 잔액이 안 줄었는데 이력만 남으면 둘이 영영 어긋난다
        verify(pointRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액과 정확히 같은 금액은 쓸 수 있다")
    void allowsSpendingExactBalance() {
        User user = user(100);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        pointService.updatePoint(USER_ID, 100, false);

        assertThat(user.getPointBalance()).isZero();
    }

    @Test
    @DisplayName("없는 사용자면 USER_NOT_FOUND")
    void rejectsUnknownUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointService.updatePoint(USER_ID, 100, true))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(pointRepository, never()).save(any());
    }

    private User user(int balance) {
        return User.builder().id(USER_ID).name("빵순이").pointBalance(balance).build();
    }

    private Point savedHistory() {
        ArgumentCaptor<Point> captor = ArgumentCaptor.forClass(Point.class);
        verify(pointRepository).save(captor.capture());
        return captor.getValue();
    }
}
