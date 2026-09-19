package com.bbangpatrol.user.service;

import com.bbangpatrol.user.repository.AccountPurgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 보관 기간이 지난 탈퇴 회원과 삭제된 리뷰를 실제로 파기한다.
 *
 * 개인정보처리방침이 약속한 "탈퇴 후 30일 뒤 파기"를 실행하는 곳이다.
 * soft delete 만 해두면 방침과 실제가 어긋나므로, 기간이 지난 행은 여기서 지운다.
 *
 * DB 작업만 한 트랜잭션으로 처리하고, R2 에 남은 이미지 key 는 호출측에 돌려준다.
 * 스토리지 삭제까지 트랜잭션에 넣으면 실패 시 되돌릴 수 없어 분리했다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountPurgeService {

    private final AccountPurgeRepository purgeRepository;

    /** 파기 결과. imageKeys 는 아직 R2 에 남아 있는 오브젝트 key 다. */
    public record PurgeResult(int purgedUsers, int purgedReviews, List<String> imageKeys) {

        public boolean isEmpty() {
            return purgedUsers == 0 && purgedReviews == 0;
        }
    }

    @Transactional
    public PurgeResult purgeExpired(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        List<Long> userIds = purgeRepository.findExpiredUserIds(cutoff);
        List<Long> reviewIds = new ArrayList<>(purgeRepository.findExpiredReviewIds(cutoff));

        // 탈퇴 회원의 리뷰는 삭제 표시가 없어도 함께 지운다
        if (!userIds.isEmpty()) {
            for (Long id : purgeRepository.findReviewIdsByUserIds(userIds)) {
                if (!reviewIds.contains(id)) {
                    reviewIds.add(id);
                }
            }
        }

        if (userIds.isEmpty() && reviewIds.isEmpty()) {
            return new PurgeResult(0, 0, List.of());
        }

        // 행을 지우면 R2 key 를 다시 찾을 수 없으므로 먼저 모아둔다
        List<String> imageKeys = new ArrayList<>();
        if (!reviewIds.isEmpty()) {
            imageKeys.addAll(purgeRepository.findReviewImageKeys(reviewIds));
        }
        if (!userIds.isEmpty()) {
            imageKeys.addAll(purgeRepository.findUserImageKeys(userIds));
        }

        // FK 때문에 자식부터 지운다
        if (!reviewIds.isEmpty()) {
            purgeRepository.deleteReviewLikesByReviewIds(reviewIds);
            purgeRepository.deleteReviewImagesByReviewIds(reviewIds);
            purgeRepository.deleteReviewKeywordsByReviewIds(reviewIds);
            purgeRepository.deleteReviewsByIds(reviewIds);
        }

        if (!userIds.isEmpty()) {
            purgeRepository.deleteReviewLikesByUserIds(userIds);
            purgeRepository.deleteVisitDetailsByUserIds(userIds);
            purgeRepository.deleteVisitsByUserIds(userIds);
            purgeRepository.deleteBookmarksByUserIds(userIds);
            purgeRepository.deleteUserItemsByUserIds(userIds);
            purgeRepository.deleteMissionProgressByUserIds(userIds);
            purgeRepository.deletePointHistoryByUserIds(userIds);
            purgeRepository.deleteUsersByIds(userIds);
        }

        log.info("[PURGE] 보관 기간({}일) 경과분 파기. 회원={}건, 리뷰={}건, 이미지={}개",
                retentionDays, userIds.size(), reviewIds.size(), imageKeys.size());

        return new PurgeResult(userIds.size(), reviewIds.size(), imageKeys);
    }
}
