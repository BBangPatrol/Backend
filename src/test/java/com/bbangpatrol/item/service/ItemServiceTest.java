package com.bbangpatrol.item.service;

import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.dto.DrawResultResponse;
import com.bbangpatrol.item.entity.Item;
import com.bbangpatrol.item.entity.ItemRank;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.service.MissionEvaluator;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 뽑기는 포인트를 쓰고 돌려받는 유일한 경로다. 차감과 환급이 어긋나면 포인트가 새거나 증발한다.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final long USER_ID = 6L;
    private static final int ITEM_COST = 100;
    private static final int DUPLICATE_REFUND = 20;

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private R2Service r2Service;
    @Mock
    private MissionEvaluator missionEvaluator;
    @Mock
    private PointService pointService;

    @InjectMocks
    private ItemService itemService;

    @Test
    @DisplayName("새 수집품을 뽑으면 100포인트를 쓰고 도감에 저장된다")
    void savesNewItemAndChargesCost() {
        givenSingleDrawableItem();
        when(userItemRepository.existsByUserIdAndItemId(USER_ID, 1L)).thenReturn(false);

        DrawResultResponse result = itemService.drawItem(USER_ID);

        verify(pointService).updatePoint(USER_ID, ITEM_COST, false, "수집품 뽑기");
        assertThat(result.duplicated()).isFalse();
        assertThat(result.refundPoint()).isZero();

        ArgumentCaptor<UserItem> captor = ArgumentCaptor.forClass(UserItem.class);
        verify(userItemRepository).save(captor.capture());
        assertThat(captor.getValue().getItem().getId()).isEqualTo(1L);

        // 새로 얻었으니 환급은 없어야 한다
        verify(pointService, never()).updatePoint(anyLong(), anyInt(), org.mockito.ArgumentMatchers.eq(true), any());
    }

    @Test
    @DisplayName("이미 가진 수집품이면 20포인트를 돌려주고 중복 저장하지 않는다")
    void refundsOnDuplicateAndSkipsSave() {
        givenSingleDrawableItem();
        when(userItemRepository.existsByUserIdAndItemId(USER_ID, 1L)).thenReturn(true);

        DrawResultResponse result = itemService.drawItem(USER_ID);

        verify(pointService).updatePoint(USER_ID, ITEM_COST, false, "수집품 뽑기");
        verify(pointService).updatePoint(USER_ID, DUPLICATE_REFUND, true, "중복 수집품 환급");
        assertThat(result.duplicated()).isTrue();
        assertThat(result.refundPoint()).isEqualTo(DUPLICATE_REFUND);

        // 같은 아이템이 도감에 두 줄로 쌓이면 수집 개수가 부풀려진다
        verify(userItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("뽑기 결과에 아이템 정보와 남은 포인트를 함께 내려준다")
    void returnsItemInfoAndRemainingBalance() {
        User user = givenSingleDrawableItem();
        when(userItemRepository.existsByUserIdAndItemId(USER_ID, 1L)).thenReturn(false);
        when(r2Service.getPublicUrl("items/1.png")).thenReturn("https://cdn/items/1.png");
        // 실제로는 pointService 가 차감하지만, 여기서는 목이라 잔액을 직접 맞춰 둔다
        user.usePoint(ITEM_COST);

        DrawResultResponse result = itemService.drawItem(USER_ID);

        assertThat(result.collectibleId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("소금빵 뱃지");
        assertThat(result.rank()).isEqualTo(ItemRank.RARE);
        assertThat(result.image()).isEqualTo("https://cdn/items/1.png");
        assertThat(result.currentPoint()).isEqualTo(400);
    }

    @Test
    @DisplayName("뽑기는 미션 진행도를 갱신한다")
    void updatesMissionProgress() {
        givenSingleDrawableItem();
        when(userItemRepository.existsByUserIdAndItemId(USER_ID, 1L)).thenReturn(false);

        itemService.drawItem(USER_ID);

        verify(missionEvaluator).onItemDrawn(USER_ID);
    }

    @Test
    @DisplayName("없는 사용자는 포인트를 쓰기 전에 막힌다")
    void rejectsUnknownUserBeforeCharging() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.drawItem(USER_ID))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(pointService, never()).updatePoint(anyLong(), anyInt(), org.mockito.ArgumentMatchers.anyBoolean(), any());
        verify(userItemRepository, never()).save(any());
    }

    private User givenSingleDrawableItem() {
        User user = User.builder().id(USER_ID).name("빵순이").pointBalance(500).build();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(itemRepository.findAll()).thenReturn(List.of(item()));
        lenient().when(r2Service.getPublicUrl(any())).thenReturn("https://cdn/items/1.png");
        return user;
    }

    private Item item() {
        return Item.builder()
                .id(1L)
                .name("소금빵 뱃지")
                .rank(ItemRank.RARE)
                .imageUrl("items/1.png")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
