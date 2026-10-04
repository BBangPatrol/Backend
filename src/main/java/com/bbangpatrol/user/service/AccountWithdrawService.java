package com.bbangpatrol.user.service;

import com.bbangpatrol.auth.service.AuthService;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.entity.ReviewLike;
import com.bbangpatrol.review.repository.ReviewLikeRepository;
import com.bbangpatrol.review.repository.ReviewRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 회원 탈퇴.
 *
 * 행을 바로 지우지 않는다. 개인정보처리방침이 "탈퇴 후 30일 보관 뒤 파기"를 약속했고,
 * 실제 삭제는 AccountPurgeService 가 기간이 지난 뒤에 한다.
 *
 * 다만 화면에서 사라져야 하는 것과, 남겨두면 다른 이용자에게 잘못 보이는 값은 지금 정리한다.
 *  - 내가 쓴 리뷰: 소프트 삭제. 남겨두면 탈퇴한 사람의 닉네임이 계속 노출된다.
 *  - 빵집 평점: 리뷰가 빠졌으니 다시 계산한다. 그러지 않으면 없는 리뷰가 평점에 남는다.
 *  - 내가 누른 좋아요: 회수하고 남의 리뷰 like_count 를 낮춘다. 30일 뒤 파기 때 행만 지우면 수치가 어긋난다.
 *  - 토큰: 리프레시 토큰을 지우고 액세스 토큰을 블랙리스트에 올린다. 그러지 않으면 탈퇴 후에도 남은 토큰으로 API 를 쓸 수 있다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountWithdrawService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final BakeryRepository bakeryRepository;
    private final AuthService authService;

    @Transactional
    public void withdraw(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 계정에 다시 요청이 오면 deletedAt 만 뒤로 밀려 보관 기간이 계속 연장된다
        if (user.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. 내가 쓴 리뷰를 내리고, 그 빵집들의 평점을 다시 계산한다
        List<Review> reviews = reviewRepository.findAllByUser_IdAndDeletedAtIsNull(userId);
        Set<Long> bakeryIds = new LinkedHashSet<>();
        for (Review review : reviews) {
            bakeryIds.add(review.getBakery().getId());
            review.softDelete();
        }
        for (Long bakeryId : bakeryIds) {
            // 마지막 리뷰였다면 평점이 NULL 이 된다 (별점 없음)
            bakeryRepository.refreshAvgRating(bakeryId);
        }

        // 2. 내가 누른 좋아요를 회수한다
        List<ReviewLike> likes = reviewLikeRepository.findAllByUser_Id(userId);
        for (ReviewLike like : likes) {
            Review liked = like.getReview();
            // 이미 지워진 리뷰의 수치는 건드리지 않는다 (지울 때 이미 화면에서 빠졌다)
            if (liked != null && liked.getDeletedAt() == null) {
                liked.decreaseLikeCount();
            }
        }
        reviewLikeRepository.deleteAll(likes);

        // 3. 탈퇴 표시. 포인트·수집품·방문 기록은 보관 기간이 지나면 함께 파기된다
        user.withdraw();

        // 4. 남아 있는 토큰 무효화
        authService.logout(userId, accessToken);

        log.info("[WITHDRAW] 회원 탈퇴 처리. userId={}, 내린 리뷰={}건, 회수한 좋아요={}건",
                userId, reviews.size(), likes.size());
    }
}
