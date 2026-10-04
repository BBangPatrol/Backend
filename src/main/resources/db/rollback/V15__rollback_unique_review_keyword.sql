-- =============================================================
-- V15 롤백 — review_keyword 유니크 제거 + 지운 중복 행 복원
--
-- Flyway(커뮤니티)는 undo 가 없고 spring.flyway.locations 는 classpath:db/migration
-- 이라 이 폴더는 스캔되지 않는다. DB 에 직접 붙어 순서대로 실행할 것.
--
-- 코드도 함께 되돌려야 완전히 원래대로 돌아간다. ReviewService.addKeyword 가 이미
-- 달린 키워드를 건너뛰므로, 코드를 남겨 두면 중복이 다시 생기지는 않는다.
--   git revert <fix: 리뷰 키워드가 중복 저장되던 문제 수정 커밋>
-- =============================================================

-- 1) 유니크 제거
ALTER TABLE review_keyword
    DROP INDEX uk_review_keyword_review_keyword;

-- 2) 백필 때 지운 중복 행 복원 (백업이 비어 있으면 0 행이 정상이다)
INSERT IGNORE INTO review_keyword (id, review_id, keyword_id)
SELECT id, review_id, keyword_id
FROM review_keyword_dup_backup_v15;

-- 3) 복원 결과 확인 (백업 건수와 같아야 한다)
SELECT (SELECT COUNT(*) FROM review_keyword_dup_backup_v15) AS backup_rows,
       (SELECT COUNT(*)
        FROM review_keyword rk
                 JOIN review_keyword_dup_backup_v15 bk ON bk.id = rk.id) AS restored_rows;

-- 4) Flyway 이력에서 V15 제거 (다시 적용할 수 있게)
DELETE
FROM flyway_schema_history
WHERE version = '15';

-- 5) 복원을 확인한 뒤 스냅샷 정리. 이 줄까지 실행하면 더는 되돌릴 수 없다
-- DROP TABLE review_keyword_dup_backup_v15;
