package com.bbangpatrol.user.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.common.dto.PageInfo;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.Point;
import com.bbangpatrol.point.repository.PointRepository;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.dto.UserRequestDTO;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.entity.Visit;
import com.bbangpatrol.visit.repository.VisitRepository;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final VisitRepository visitRepository;
    private final MissionProgressRepository missionProgressRepository;
    private final R2Service r2Service;

    private static final List<String> ALLOWED_PROFILE_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    // 프로필 이미지는 아바타로만 쓰이므로 400px 한 장이면 충분하다 (리뷰 목록에 20개가 함께 뜬다)
    private static final int PROFILE_IMAGE_MAX_DIMENSION = 400;
    private static final float PROFILE_IMAGE_QUALITY = 0.8f;
    private static final int POINT_HISTORY_PAGE_SIZE = 20;
    private static final int REVIEW_HISTORY_PAGE_SIZE = 20;
    private static final String USERS_DIR = "users";

    @Transactional
    public UserResponseDTO.MyPageDTO getMyPage(Long userId) {
        User user = getUser(userId);

        Long total = itemRepository.count();
        Long collected = userItemRepository.countByUser(user);
        List<UserItem> userItemList = userItemRepository.findTop8ByUserOrderByAcquiredAtDescIdDesc(user);

        List<Visit> visitList = visitRepository.findByUser(user);

        Long reviewCnt = reviewRepository.countByUserAndDeletedAtIsNull(user);
        Long reviewLikes = reviewLikeRepository.countLikes(user);

        List<MissionProgress> progressList = missionProgressRepository.findByUserOrderByStatusAndUpdatedAt(user);

        return UserResponseDTO.MyPageDTO.builder()
                .nickname(user.getName())
                .collectionBooks(UserResponseDTO.CollectionBook.builder()
                        .collected(Math.toIntExact(collected))
                        .total(Math.toIntExact(total))
                        .items(userItemList.stream().map(ui -> UserResponseDTO.CollectionItem.builder()
                                .collectibleId(ui.getItem().getId())
                                .name(ui.getItem().getName())
                                .rank(ui.getItem().getRank())
                                .image(r2Service.getPublicUrl(ui.getItem().getImageUrl())).build()).toList()).build())
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

    @Transactional
    public void editNickname(Long userId, UserRequestDTO.EditNicknameDTO request) {
        User user = getUser(userId);
        if (userRepository.existsByName(request.getNickname())) throw new ApiException(ErrorCode.NICKNAME_ALREADY_EXISTS);

        user.updateNickname(request.getNickname());
    }

    public UserResponseDTO.ProfileImageDTO getMyProfileImage(Long userId) {
        User user = getUser(userId);
        return UserResponseDTO.ProfileImageDTO.builder()
                .imageUrl(r2Service.getPublicUrl(user.getUserImage())).build();
    }

    @Transactional
    public void postProfileImage(Long userId, MultipartFile request) throws IOException {
        User user = getUser(userId);
        if(request == null || request.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST);
        if(!ALLOWED_PROFILE_IMAGE_TYPES.contains(request.getContentType())) throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE415);

        String previousKey = user.getUserImage();

        // 저장하는 값은 키다. URL 변환은 조회하는 쪽에서 getPublicUrl 로 한다.
        // 원본 대신 400px JPEG 로 줄여 올린다
        String key = r2Service.uploadResizedImage(
                request, USERS_DIR + "/" + user.getId(), PROFILE_IMAGE_MAX_DIMENSION, PROFILE_IMAGE_QUALITY);
        user.updateImage(key);

        deletePreviousImageAfterCommit(userId, previousKey, key);
    }

    // 커밋된 이후에 이전 프로필 이미지를 지운다. 롤백되면 지우지 않는다
    private void deletePreviousImageAfterCommit(Long userId, String previousKey, String newKey) {
        if (!StringUtils.hasText(previousKey) || previousKey.equals(newKey)) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    r2Service.deleteFile(previousKey);
                } catch (Exception e) {
                    log.error("이전 프로필 이미지 삭제가 실패했습니다. userId={}, key={}", userId, previousKey);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.PointHistoryDTO getPointHistory(Long userId, Long cursor) {
        getUser(userId);

        List<Point> points = pointRepository.findPointHistory(userId, cursor, PageRequest.of(0, POINT_HISTORY_PAGE_SIZE + 1));

        boolean hasNext = points.size() > POINT_HISTORY_PAGE_SIZE;
        List<Point> page = hasNext ? points.subList(0, POINT_HISTORY_PAGE_SIZE) : points;

        List<UserResponseDTO.PointDTO> pointHistory = page.stream()
                .map(point -> UserResponseDTO.PointDTO.builder()
                        .type(point.getType().name())
                        .content(point.getContent())
                        .amount(point.getAmount())
                        .date(point.getCreatedAt())
                        .build())
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        return UserResponseDTO.PointHistoryDTO.builder()
                .point_history(pointHistory)
                .pageInfo(new PageInfo(pointHistory.size(), hasNext, nextCursor))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.ReviewHistoryDTO getMyReviews(Long userId, Long cursor) {
        User user = getUser(userId);

        List<Review> reviews = reviewRepository.findMyReviews(userId, cursor, PageRequest.of(0, REVIEW_HISTORY_PAGE_SIZE + 1));

        boolean hasNext = reviews.size() > REVIEW_HISTORY_PAGE_SIZE;
        List<Review> page = hasNext ? reviews.subList(0, REVIEW_HISTORY_PAGE_SIZE) : reviews;

        List<UserResponseDTO.ReviewDTO> reviewHistory = page.stream()
                .map(review -> UserResponseDTO.ReviewDTO.builder()
                        .bakeryId(review.getBakery().getId())
                        .bakeryName(review.getBakery().getName())
                        .rating(review.getRating())
                        .content(review.getContent())
                        .likeCount(review.getLikeCount())
                        .date(review.getCreatedAt())
                        .build())
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        return UserResponseDTO.ReviewHistoryDTO.builder()
                .reviews(reviewHistory)
                .reviewCount(reviewRepository.countByUserAndDeletedAtIsNull(user))
                .reviewLikes(reviewRepository.sumLikeCountByUser(user))
                .pageInfo(new PageInfo(reviewHistory.size(), hasNext, nextCursor))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.VisitedBakeryListDTO getBakeryList(Long userId) {
        User user = getUser(userId);

        List<UserResponseDTO.Coordinate> visits = visitRepository.findByUser(user).stream()
                .map(Visit::getBakery)
                .filter(Objects::nonNull)
                .map(bakery -> UserResponseDTO.Coordinate.builder()
                        .lat(bakery.getLat())
                        .lon(bakery.getLng())
                        .build())
                .toList();

        return UserResponseDTO.VisitedBakeryListDTO.builder()
                .visits(visits)
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
