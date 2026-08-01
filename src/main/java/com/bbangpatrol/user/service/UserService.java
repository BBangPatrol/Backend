package com.bbangpatrol.user.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.PointHistory;
import com.bbangpatrol.point.entity.PointType;
import com.bbangpatrol.point.repository.PointHistoryRepository;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.repository.VisitRepository;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final VisitRepository visitRepository;
    private final MissionProgressRepository missionProgressRepository;

    @Transactional
    public void addPoint(Long userId, Integer point, PointType type, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.addPoint(point);
        pointHistoryRepository.save(PointHistory.builder()
                .user(user)
                .type(type)
                .amount(point)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public UserResponseDTO.MyPageDTO getMyPage(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Long total = itemRepository.count();
        Long collected = userItemRepository.countByUser(user);
        List<UserItem> userItemList = userItemRepository.findTop8ByUserOrderByAcquiredAtDescIdDesc(user);

        List<Visit> visitList = visitRepository.findByUser(user);

        Long reviewCnt = reviewRepository.countByUser(user);
        Long reviewLikes = reviewLikeRepository.countLikes(user);

        List<MissionProgress> progressList = missionProgressRepository.findByUserOrderByStatusAndUpdatedAt(user);

        return UserResponseDTO.MyPageDTO.builder()
                .nickname(user.getName())
                .collectionBooks(UserResponseDTO.CollectionBook.builder()
                        .collected(Math.toIntExact(collected))
                        .total(Math.toIntExact(total))
                        .items(userItemList.stream().map(ui -> UserResponseDTO.CollectionItem.builder()
                                .id(ui.getItem().getId())
                                .name(ui.getItem().getName())
                                .url(ui.getItem().getImageUrl()).build()).toList()).build())
                .map(visitList.stream().map(visit -> {
                    Bakery bakery = visit.getBakery();

                    return UserResponseDTO.Coordinate.builder()
                            .lat(bakery.getLat())
                            .lon(bakery.getLng()).build();
                }).toList())
                .point(user.getPointBalance())
                .reviews(UserResponseDTO.ReviewStat.builder()
                        .reviewCount(reviewCnt)
                        .reviewLikes(reviewLikes).build())
                .missions(progressList.stream().map(progress -> UserResponseDTO.MissionDTO.builder()
                        .missionId(progress.getMission().getId())
                        .title(progress.getMission().getTitle())
                        .count(progress.getCount())
                        .targetCount(progress.getMission().getTargetCount())
                        .build()).toList())
                .build();
    }
}
