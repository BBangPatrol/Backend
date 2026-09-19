package com.bbangpatrol.user.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.common.dto.OffsetPageInfo;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.item.repository.ItemRepository;
import com.bbangpatrol.item.repository.UserItemRepository;
import com.bbangpatrol.mission.entity.MissionProgress;
import com.bbangpatrol.point.entity.Point;
import com.bbangpatrol.point.repository.PointRepository;
import com.bbangpatrol.review.entity.Keyword;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewImage;
import com.bbangpatrol.review.entity.ReviewKeyword;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.dto.UserRequestDTO;
import com.bbangpatrol.user.dto.UserResponseDTO;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.repository.VisitDetailRepository;
import com.bbangpatrol.mission.repository.MissionProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    private final VisitDetailRepository visitDetailRepository;
    private final MissionProgressRepository missionProgressRepository;
    private final R2Service r2Service;

    private static final List<String> ALLOWED_PROFILE_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final int PROFILE_IMAGE_MAX_DIMENSION = 400;
    private static final float PROFILE_IMAGE_QUALITY = 0.8f;
    private static final String USERS_DIR = "users";

    @Transactional
    public UserResponseDTO.MyPageDTO getMyPage(Long userId) {
        User user = getUser(userId);

        Long total = itemRepository.count();
        Long collected = userItemRepository.countByUser(user);
        List<UserItem> userItemList = userItemRepository.findTop8ByUserOrderByAcquiredAtDescIdDesc(user);

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
                                .rank(ui.getItem().getRank().toString())
                                .image(r2Service.getPublicUrl(ui.getItem().getImageUrl())).build()).toList()).build())
                .point(user.getPointBalance())
                .reviews(UserResponseDTO.ReviewStat.builder()
                        .reviewCount(reviewCnt)
                        .reviewLikes(reviewLikes).build())
                .missions(progressList.stream().map(progress -> UserResponseDTO.MissionDTO.builder()
                        .missionId(progress.getMission().getId())
                        .title(progress.getMission().getTitle())
                        .count(progress.getCount())
                        .targetCount(progress.getMission().getTargetCount())
                        .status(progress.getStatus().toString())
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
    public UserResponseDTO.PointHistoryDTO getPointHistory(Long userId, int page) {
        // PageRequest.of 가 음수에 IllegalArgumentException 을 던져 500 이 된다
        if (page < 0) throw new ApiException(ErrorCode.BAD_REQUEST);

        getUser(userId);

        Page<Point> points = pointRepository.findPointHistory(userId, PageRequest.of(page, 5));
        List<UserResponseDTO.PointDTO> pointHistory = points.getContent().stream()
                .map(point -> UserResponseDTO.PointDTO.builder()
                        .type(point.getType().name())
                        .content(point.getContent())
                        .amount(point.getAmount())
                        .date(point.getCreatedAt())
                        .build())
                .toList();

        return UserResponseDTO.PointHistoryDTO.builder()
                .point_history(pointHistory)
                .pageInfo(new OffsetPageInfo(
                        points.getNumber(),
                        points.getSize(),
                        points.getTotalElements(),
                        points.getTotalPages(),
                        points.hasNext()))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.ReviewHistoryDTO getMyReviews(Long userId, int page) {
        if (page < 0) throw new ApiException(ErrorCode.BAD_REQUEST);

        User user = getUser(userId);

        Page<Review> reviews = reviewRepository.findMyReviews(userId, PageRequest.of(page, 5));
        List<UserResponseDTO.ReviewDTO> reviewHistory = reviews.getContent().stream()
                .map(review -> UserResponseDTO.ReviewDTO.builder()
                        .bakeryId(review.getBakery().getId())
                        .bakeryName(review.getBakery().getName())
                        .rating(review.getRating())
                        .content(review.getContent())
                        .keywords(review.getReviewKeywords().stream().map(ReviewKeyword::getKeyword).map(Keyword::getId).toList())
                        .images(review.getReviewImages().stream().map(ReviewImage::getImageUrl).map(r2Service::getPublicUrl).toList())
                        .thumbnails(review.getReviewImages().stream().map(ReviewImage::getThumbnailUrl).map(r2Service::getPublicUrl).toList())
                        .likeCount(review.getLikeCount())
                        .isLike(reviewLikeRepository.findByUserAndReview(user, review) != null)
                        .date(review.getCreatedAt())
                        .build())
                .toList();

        return UserResponseDTO.ReviewHistoryDTO.builder()
                .reviews(reviewHistory)
                .reviewCount(reviews.getTotalElements())
                .reviewLikes(reviewRepository.sumLikeCountByUser(user))
                .pageInfo(new OffsetPageInfo(
                        reviews.getNumber(),
                        reviews.getSize(),
                        reviews.getTotalElements(),
                        reviews.getTotalPages(),
                        reviews.hasNext()))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.VisitedBakeryListDTO getBakeryList(Long userId, String query) {
        getUser(userId);

        List<UserResponseDTO.VisitBakeryDTO> data = visitDetailRepository.findHistoryByUserId(userId, query).stream()
                .map(visitDetail -> {
                    Bakery bakery = visitDetail.getVisit().getBakery();

                    Review review = visitDetail.getReview();
                    if (review != null && review.getDeletedAt() != null) review = null;

                    LocalDate today = LocalDate.now();
                    LocalDate visitedAt = visitDetail.getVisitedAt();
                    LocalDate deadline = visitedAt.plusDays(7);

                    String state = review != null ? "reviewed" : deadline.isBefore(today) ? "expired" : "none";

                    return UserResponseDTO.VisitBakeryDTO.builder()
                            .visitDetailId(visitDetail.getId())
                            .storeId(bakery.getId())
                            .storeName(bakery.getName())
                            .storeImageUrl(r2Service.getPublicUrl(bakery.getSignatureImages().get(0).getImageUrl()))
                            .visitDate(visitedAt.toString())
                            .state(state)
                            .review(review == null ? null : UserResponseDTO.ReviewInfoDTO.builder()
                                    .id(review.getId())
                                    .rating(review.getRating())
                                    .content(review.getContent())
                                    .keywords(review.getReviewKeywords().stream().map(ReviewKeyword::getKeyword).map(Keyword::getId).toList())
                                    .images(review.getReviewImages().stream().map(ReviewImage::getImageUrl).map(r2Service::getPublicUrl).toList())
                                    .thumbnails(review.getReviewImages().stream().map(ReviewImage::getThumbnailUrl).map(r2Service::getPublicUrl).toList()).build())
                            .reviewDeadline(deadline.toString())
                            .remainingDays(Math.max(ChronoUnit.DAYS.between(today, deadline), 0))
                            .build();
                })
                .toList();

        return new UserResponseDTO.VisitedBakeryListDTO(data);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
