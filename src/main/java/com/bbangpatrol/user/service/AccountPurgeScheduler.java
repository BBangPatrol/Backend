package com.bbangpatrol.user.service;

import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.user.service.AccountPurgeService.PurgeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 파기 작업을 하루 한 번 돌린다.
 *
 * DB 트랜잭션(AccountPurgeService)이 끝난 뒤에 스토리지를 지운다.
 * 순서를 뒤집으면 트랜잭션이 실패했을 때 이미 지운 이미지를 되살릴 수 없다.
 * 반대로 이 순서에서는 스토리지 삭제가 실패해도 고아 파일이 남을 뿐이라 로그로 추적할 수 있다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "account.purge.enabled", havingValue = "true", matchIfMissing = true)
public class AccountPurgeScheduler {

    private final AccountPurgeService accountPurgeService;
    private final R2Service r2Service;

    @Value("${account.purge.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${account.purge.cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void purge() {
        PurgeResult result;
        try {
            result = accountPurgeService.purgeExpired(retentionDays);
        } catch (Exception e) {
            // 다음 실행에서 다시 시도한다. 여기서 스케줄러가 죽으면 파기가 영원히 멈춘다
            log.error("[PURGE] 파기 작업 실패", e);
            return;
        }

        if (result.isEmpty()) {
            return;
        }

        int deleted = 0;
        for (String key : result.imageKeys()) {
            if (key == null || key.isBlank()) continue;
            try {
                r2Service.deleteFile(key);
                deleted++;
            } catch (Exception e) {
                log.warn("[PURGE] 이미지 삭제 실패. key={}", key, e);
            }
        }

        log.info("[PURGE] 완료. 회원={}건, 리뷰={}건, 이미지 삭제={}/{}개",
                result.purgedUsers(), result.purgedReviews(), deleted, result.imageKeys().size());
    }
}
