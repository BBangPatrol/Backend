package com.bbangpatrol.item.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.item.dto.DrawResult;
import com.bbangpatrol.item.dto.DrawResultResponse;
import com.bbangpatrol.item.dto.ItemListResponse;
import com.bbangpatrol.item.dto.ItemResponse;
import com.bbangpatrol.item.entity.Item;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.service.MissionEvaluator;
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

    private final int ITEM_COST = 100;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final R2Service r2Service;
    private final MissionEvaluator missionEvaluator;

    @Transactional
    public DrawResult drawItem(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저입니다."));

        if (user.getPointBalance() < ITEM_COST) throw new IllegalStateException("사용자의 잔액이 부족합니다.");
        
        Item drawItem = drawRandomItem();
        boolean isDuplicated = userItemRepository.existsByUserIdAndItemId(userId, drawItem.getId());
        if (isDuplicated) {
            user.addPoint(5);
        } else {
            userItemRepository.save(UserItem.builder()
                    .acquiredAt(LocalDateTime.now())
                    .user(user)
                    .item(drawItem)
                    .build());
        }
        // 수집품 미션 진행도 갱신
        missionEvaluator.onItemDrawn(userId);

        DrawResultResponse response = new DrawResultResponse(drawItem.getId(), user.getPointBalance());
        return new DrawResult(response, isDuplicated);
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
                        r2Service.getPublicUrl(item.getImageUrl())
                )).toList();
        return new ItemListResponse(itemResponses, (long) itemResponses.size());
    }
}
