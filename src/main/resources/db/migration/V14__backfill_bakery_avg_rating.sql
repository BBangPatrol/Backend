-- =============================================================
-- V14: bakery.avg_rating 백필
--
-- 평점은 리뷰에서 계산되지 않고 bakery 행에 저장된 값을 그대로 내려준다(V9 주석).
-- 그런데 그 값을 채우는 코드가 없어서, V9 가 손수 넣은 시연점을 빼면 리뷰가 달린
-- 가게도 avg_rating 이 NULL 로 남아 있다. 지도 목록 별점은 "-" 로 나오고
-- 검색 sort=rating 에서는 순위 밖으로 밀린다.
--
-- 이제 ReviewService 가 리뷰 생성/수정/삭제마다
-- BakeryRepository.refreshAvgRating() 으로 같은 계산을 하므로, 이미 쌓여 있는
-- 리뷰에 대해서도 여기서 한 번 맞춰 둔다. 계산식은 그쪽과 같게 유지할 것.
--
-- 되돌리려면 db/rollback/V14__rollback_backfill_bakery_avg_rating.sql 을 쓴다.
-- Flyway(커뮤니티)는 undo 가 없어서 덮어쓰기 전 값을 bakery_avg_rating_backup_v14
-- 에 남겨 둔다. 롤백을 확인한 뒤 그 테이블은 지우면 된다.
-- =============================================================

-- 롤백용 스냅샷. 재적용되는 DB 가 있어도 처음 백업을 덮지 않도록 IF NOT EXISTS / IGNORE 로 둔다
CREATE TABLE IF NOT EXISTS bakery_avg_rating_backup_v14
(
    bakery_id    BIGINT       NOT NULL,
    avg_rating   DECIMAL(2, 1) NULL,
    backed_up_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bakery_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO bakery_avg_rating_backup_v14 (bakery_id, avg_rating)
SELECT b.id, b.avg_rating
FROM bakery b;

-- 살아 있는 리뷰(deleted_at IS NULL)의 평균으로 다시 계산한다.
-- LEFT JOIN 이라 리뷰가 하나도 없는 가게는 NULL(별점 없음)이 된다.
-- <=> 는 NULL 안전 비교다. 값이 실제로 바뀌는 행만 건드려 updated_at 이 의미 없이 밀리지 않게 한다.
UPDATE bakery b
    LEFT JOIN (SELECT r.bakery_id,
                      ROUND(AVG(r.rating), 1) AS avg_rating
               FROM review r
               WHERE r.deleted_at IS NULL
               GROUP BY r.bakery_id) live ON live.bakery_id = b.id
SET b.avg_rating = live.avg_rating,
    b.updated_at = NOW()
WHERE NOT (b.avg_rating <=> live.avg_rating);
