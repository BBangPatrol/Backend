package com.bbangpatrol.item.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.item.dto.DrawResultResponse;
import com.bbangpatrol.item.dto.ItemListResponse;
import com.bbangpatrol.item.dto.ItemResponse;
import com.bbangpatrol.item.entity.Item;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.service.MissionEvaluator;
import com.bbangpatrol.point.service.PointService;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int ITEM_COST = 100;
    private static final int DUPLICATE_REFUND_POINT = 20;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final R2Service r2Service;
    private final MissionEvaluator missionEvaluator;
    private final PointService pointService;

    @Transactional
    public DrawResultResponse drawItem(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저입니다."));

        pointService.updatePoint(userId, ITEM_COST, false, "수집품 뽑기");
        
        Item drawItem = drawRandomItem();
        boolean isDuplicated = userItemRepository.existsByUserIdAndItemId(userId, drawItem.getId());
        int refundPoint = 0;
        if (isDuplicated) {
            pointService.updatePoint(userId, DUPLICATE_REFUND_POINT, true, "중복 수집품 환급");
            refundPoint = DUPLICATE_REFUND_POINT;
        } else {
            userItemRepository.save(UserItem.builder()
                    .acquiredAt(LocalDateTime.now())
                    .user(user)
                    .item(drawItem)
                    .build());
        }
        // 수집품 미션 진행도 갱신
        missionEvaluator.onItemDrawn(userId);

        // 결과 화면이 도감 목록을 다시 받지 않아도 되도록 아이템 정보를 함께 내려준다
        return new DrawResultResponse(
                drawItem.getId(),
                drawItem.getName(),
                drawItem.getRank(),
                r2Service.getPublicUrl(drawItem.getImageUrl()),
                isDuplicated,
                refundPoint,
                user.getPointBalance());
    }

    private Item drawRandomItem() {
        List<Item> items = itemRepository.findAll();
        int index = ThreadLocalRandom.current().nextInt(items.size());
        return items.get(index);
    }


    public ItemListResponse getItems(Long userId, String type) {

        List<Item> items = "all".equals(type)
                ? itemRepository.findAll()
                : userItemRepository.findItemsByUserId(userId);

        List<ItemResponse> itemResponses = items.stream()
                .map(item -> new ItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getRank(),
                        r2Service.getPublicUrl(item.getImageUrl())
                )).toList();
        return new ItemListResponse(itemResponses, (long) itemResponses.size());
    }
}
