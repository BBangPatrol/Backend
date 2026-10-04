-- =============================================================
-- V15: review_keyword 에 (review_id, keyword_id) 유니크 추가
--
-- review_like(user_id, review_id) / bookmark / mission_progress / visits 는 모두
-- 유니크가 걸려 있는데 review_keyword 만 없었다. ReviewService.addKeyword 가 존재
-- 여부를 보지 않고 저장해서, 같은 키워드를 다시 보내면 행이 쌓이고 리뷰 조회 응답의
-- keywords 배열에 같은 id 가 여러 번 실렸다. 서비스 쪽도 같은 커밋에서 막았고,
-- 여기서는 DB 가 다시 못 생기게 잠근다.
--
-- 되돌리려면 db/rollback/V15__rollback_unique_review_keyword.sql 을 쓴다.
-- 지우는 행은 review_keyword_dup_backup_v15 에 남겨 둔다.
-- (운영 DB 확인 시점 기준 중복 행은 0건이라 실제로 지워지는 행은 없다)
-- =============================================================

-- 지울 중복 행 스냅샷. 재적용되는 DB 가 있어도 처음 백업을 덮지 않도록 IF NOT EXISTS / IGNORE 로 둔다
CREATE TABLE IF NOT EXISTS review_keyword_dup_backup_v15
(
    id           BIGINT   NOT NULL,
    review_id    BIGINT   NOT NULL,
    keyword_id   BIGINT   NOT NULL,
    backed_up_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO review_keyword_dup_backup_v15 (id, review_id, keyword_id)
SELECT rk.id, rk.review_id, rk.keyword_id
FROM review_keyword rk
         JOIN (SELECT review_id, keyword_id, MIN(id) AS keep_id
               FROM review_keyword
               GROUP BY review_id, keyword_id
               HAVING COUNT(*) > 1) dup
              ON dup.review_id = rk.review_id AND dup.keyword_id = rk.keyword_id
WHERE rk.id > dup.keep_id;

-- 같은 (리뷰, 키워드) 중 가장 먼저 들어온 행만 남긴다
DELETE rk
FROM review_keyword rk
         JOIN (SELECT review_id, keyword_id, MIN(id) AS keep_id
               FROM review_keyword
               GROUP BY review_id, keyword_id
               HAVING COUNT(*) > 1) dup
              ON dup.review_id = rk.review_id AND dup.keyword_id = rk.keyword_id
WHERE rk.id > dup.keep_id;

-- 실패 후 재적용되는 DB 에는 제약이 이미 있을 수 있어 존재 여부를 보고 추가한다 (V13 과 같은 방식)
SET @exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'review_keyword'
      AND constraint_name = 'uk_review_keyword_review_keyword'
);

SET @sql := IF(@exists > 0,
               'SELECT 1',
               'ALTER TABLE review_keyword ADD CONSTRAINT uk_review_keyword_review_keyword UNIQUE (review_id, keyword_id)');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
