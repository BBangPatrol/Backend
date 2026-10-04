-- =============================================================
-- V14 롤백 — bakery.avg_rating 을 백필 전 값으로 되돌린다
--
-- Flyway(커뮤니티)는 undo 를 지원하지 않고, spring.flyway.locations 는
-- classpath:db/migration 이라 이 폴더는 스캔되지 않는다. 자동으로 실행되는 파일이
-- 아니므로 DB 에 직접 붙어 아래 순서대로 실행할 것.
--
-- 코드도 함께 되돌려야 한다. ReviewService 가 리뷰 생성/수정/삭제마다
-- refreshAvgRating() 을 부르므로, 코드를 남겨 두면 다음 리뷰 변경에서 그 가게만
-- 다시 계산된다.
--   git revert <feat: 리뷰가 바뀔 때 가게 평점 자동 재계산 커밋>
-- =============================================================

-- 1) 되돌릴 스냅샷이 있는지 먼저 확인한다. 0 이면 복원할 값이 없다는 뜻이니 여기서 멈출 것
SELECT COUNT(*)                        AS backup_rows,
       SUM(avg_rating IS NOT NULL)     AS backup_rated_rows
FROM bakery_avg_rating_backup_v14;

-- 2) 백필 전 값으로 복원. <=> 로 실제로 달라진 행만 건드린다
UPDATE bakery b
    JOIN bakery_avg_rating_backup_v14 bk ON bk.bakery_id = b.id
SET b.avg_rating = bk.avg_rating,
    b.updated_at = NOW()
WHERE NOT (b.avg_rating <=> bk.avg_rating);

-- 3) 복원 결과 확인 (0 행이어야 한다)
SELECT COUNT(*) AS not_restored
FROM bakery b
         JOIN bakery_avg_rating_backup_v14 bk ON bk.bakery_id = b.id
WHERE NOT (b.avg_rating <=> bk.avg_rating);

-- 4) Flyway 이력에서 V14 를 지운다. 스냅샷 테이블을 남겨 두면 재적용도 안전하다
--    (CREATE TABLE IF NOT EXISTS / INSERT IGNORE 라 처음 백업이 유지된다)
DELETE
FROM flyway_schema_history
WHERE version = '14';

-- 5) 복원을 확인한 뒤 스냅샷 정리. 이 줄까지 실행하면 더는 되돌릴 수 없다
-- DROP TABLE bakery_avg_rating_backup_v14;
