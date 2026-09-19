package com.bbangpatrol.mission.service;

import com.bbangpatrol.visit.event.ReceiptVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 영수증 인증이 커밋된 뒤에 미션 진행도를 갱신한다.
 *
 * 예전에는 인증 트랜잭션 안에서 바로 갱신해서, 미션 쪽에서 락 대기나 오류가 나면
 * 방문 인증이 통째로 롤백됐다. 보상(미션)이 본행위(인증)를 끌어내리는 구조였다.
 * 커밋 뒤로 옮기고 여기서 예외를 삼키면, 미션이 실패해도 인증과 포인트는 남는다.
 *
 * AFTER_COMMIT 리스너는 같은 스레드에서 동기로 실행되므로 응답보다 먼저 끝난다 —
 * 클라이언트가 인증 직후 미션을 다시 불러도 갱신된 값을 본다.
 * 트랜잭션은 MissionEvaluator 가 자기 것으로 새로 연다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MissionEventListener {

    private final MissionEvaluator missionEvaluator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReceiptVerified(ReceiptVerifiedEvent event) {
        try {
            missionEvaluator.onReceiptVerified(event.userId(), event.region());
        } catch (Exception e) {
            // 미션을 못 올려도 인증 자체는 이미 확정됐다. 다음 인증 때 같은 카운트로 다시 계산된다
            log.error("[MISSION] 영수증 인증 후 미션 갱신 실패. userId={}", event.userId(), e);
        }
    }
}
