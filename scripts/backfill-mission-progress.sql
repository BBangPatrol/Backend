-- =============================================================
-- mission_progress 백필
--
-- 왜 필요한가:
--   2026-09-19 22:1x (PR #96 머지 배포) ~ MissionEvaluator 수정 배포 전까지,
--   미션 갱신이 AFTER_COMMIT 리스너에서 이미 커밋된 트랜잭션에 얹히는 바람에
--   영수증 인증 / 리뷰 작성 / 수집품 뽑기에서 진행도가 한 건도 저장되지 않았다.
--
-- 왜 복구가 가능한가:
--   MissionCounter 는 진행도를 누적(+1)하지 않고 매번 원천 테이블
--   (visit_detail / visits / review / user_item)에서 다시 센다.
--   즉 진짜 값은 여전히 DB 에 다 있고, mission_progress 만 밀려 있을 뿐이다.
--   이 스크립트는 그 계산을 SQL 로 그대로 옮긴 것이다.
--
-- 안전장치:
--   - 진행도는 올리기만 한다. 줄이거나 되돌리지 않는다.
--   - status 가 in_progress 인 행만 건드린다.
--     (not_received / completed / failed = 이미 달성했거나 보상까지 받은 행은 그대로 둔다)
--   - 따라서 여러 번 돌려도 결과가 같다(멱등). 수정 배포 후에 다시 돌려도 안전하다.
--   - 포인트는 건드리지 않는다. 미션 달성은 not_received 까지만 가고,
--     보상 수령은 사용자가 직접 눌러야 PointService 가 탄다.
--
-- 주의:
--   - completed_at 은 "실제로 목표를 채운 시각"이 아니라 이 스크립트를 돌린 시각이 된다.
--     (원래 시각은 남아 있지 않아 복원 불가)
--   - 실행 전 백업:
--       mysqldump -u <user> -p --single-transaction --default-character-set=utf8mb4 \
--         bbangpatrol mission_progress > mp_before_backfill_$(date +%Y%m%d).sql
--
-- 실행:
--   mysql --default-character-set=utf8mb4 -h <host> -u bbangpatrol -p bbangpatrol \
--     < backfill-mission-progress.sql
-- =============================================================

-- -------------------------------------------------------------
-- 1) 미션별 "정답" 진행도를 계산해서 임시 테이블에 담는다.
--    조건은 MissionRepository.findTargets + MissionCounter 와 동일하게 맞췄다.
-- -------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS mp_backfill;

CREATE TEMPORARY TABLE mp_backfill AS
SELECT u.id                AS user_id,
       m.id                AS mission_id,
       m.target_count      AS target_count,
       LEAST(
           CASE m.criteria
               -- RECEIPT_COUNT: region 이 구역없음이면 전 지역, 아니면 해당 구의 영수증만
               WHEN 'RECEIPT_COUNT' THEN (
                   SELECT COUNT(*)
                     FROM visit_detail vd
                     JOIN visits v ON v.id = vd.visit_id
                     LEFT JOIN bakery b ON b.id = v.bakery_id
                    WHERE v.user_id = u.id
                      AND (m.region = '구역없음' OR b.region = m.region))

               -- DISTINCT_REGION: 인증한 서로 다른 구 수 (구역없음은 제외)
               WHEN 'DISTINCT_REGION' THEN (
                   SELECT COUNT(DISTINCT b.region)
                     FROM visits v
                     JOIN bakery b ON b.id = v.bakery_id
                    WHERE v.user_id = u.id
                      AND b.region IS NOT NULL
                      AND b.region <> '구역없음')

               -- DISTINCT_BAKERY: 방문한 서로 다른 빵집 수
               WHEN 'DISTINCT_BAKERY' THEN (
                   SELECT COUNT(DISTINCT v.bakery_id)
                     FROM visits v
                    WHERE v.user_id = u.id)

               -- REVIEW_COUNT: 삭제되지 않은 리뷰 수
               WHEN 'REVIEW_COUNT' THEN (
                   SELECT COUNT(*)
                     FROM review r
                    WHERE r.user_id = u.id
                      AND r.deleted_at IS NULL)

               -- DISTINCT_ITEM*: 보유 수집품 수 (user_item 에 (user, item) UNIQUE 가 걸려 있다)
               WHEN 'DISTINCT_ITEM' THEN (
                   SELECT COUNT(*)
                     FROM user_item ui
                    WHERE ui.user_id = u.id)

               WHEN 'DISTINCT_ITEM_RARE' THEN (
                   SELECT COUNT(*)
                     FROM user_item ui
                     JOIN item i ON i.id = ui.item_id
                    WHERE ui.user_id = u.id AND i.`rank` = 'RARE')

               WHEN 'DISTINCT_ITEM_EPIC' THEN (
                   SELECT COUNT(*)
                     FROM user_item ui
                     JOIN item i ON i.id = ui.item_id
                    WHERE ui.user_id = u.id AND i.`rank` = 'EPIC')

               WHEN 'DISTINCT_ITEM_LEGENDARY' THEN (
                   SELECT COUNT(*)
                     FROM user_item ui
                     JOIN item i ON i.id = ui.item_id
                    WHERE ui.user_id = u.id AND i.`rank` = 'LEGENDARY')

               ELSE 0
           END,
           m.target_count
       ) AS counted
  FROM `user` u
 CROSS JOIN mission m
 WHERE u.deleted_at IS NULL
   AND m.criteria <> 'NOT_SUPPORTED'
   AND (m.start_date IS NULL OR m.start_date <= CURDATE())
   AND (m.end_date   IS NULL OR m.end_date   >= CURDATE());

-- 0 짜리는 행이 없는 것과 의미가 같다. 유저 × 미션 전수 행이 생기는 걸 막는다.
DELETE FROM mp_backfill WHERE counted <= 0;

CREATE INDEX idx_mp_backfill ON mp_backfill (user_id, mission_id);

-- -------------------------------------------------------------
-- 2) 실행 전 확인 (DRY RUN)
--    아래 두 쿼리 결과가 이 스크립트가 바꿀 내용의 전부다.
-- -------------------------------------------------------------
-- MySQL 은 한 쿼리에서 같은 임시 테이블을 두 번 못 읽는다(Can't reopen table).
-- 그래서 UNION 으로 묶지 않고 따로 센다.
SELECT '새로 만들 행' AS kind, COUNT(*) AS cnt
  FROM mp_backfill b
 WHERE NOT EXISTS (SELECT 1 FROM mission_progress mp
                    WHERE mp.user_id = b.user_id AND mp.mission_id = b.mission_id);

SELECT '올릴 기존 행' AS kind, COUNT(*) AS cnt
  FROM mp_backfill b
  JOIN mission_progress mp
    ON mp.user_id = b.user_id AND mp.mission_id = b.mission_id
 WHERE mp.status = 'in_progress'
   AND b.counted > mp.`count`;

SELECT b.user_id, b.mission_id, m.title,
       mp.`count` AS before_count, b.counted AS after_count,
       COALESCE(mp.status, '(행 없음)') AS before_status,
       CASE WHEN b.counted >= b.target_count THEN 'not_received' ELSE 'in_progress' END AS after_status
  FROM mp_backfill b
  JOIN mission m ON m.id = b.mission_id
  LEFT JOIN mission_progress mp
    ON mp.user_id = b.user_id AND mp.mission_id = b.mission_id
 WHERE mp.id IS NULL
    OR (mp.status = 'in_progress' AND b.counted > mp.`count`)
 ORDER BY b.user_id, b.mission_id;

-- -------------------------------------------------------------
-- 3) 실제 반영
--    위 DRY RUN 결과를 확인한 뒤 아래 블록의 주석을 풀고 다시 실행한다.
-- -------------------------------------------------------------
-- START TRANSACTION;
--
-- -- 3-1) 없던 행 생성
-- INSERT INTO mission_progress (user_id, mission_id, `count`, status, completed_at, created_at, updated_at)
-- SELECT b.user_id,
--        b.mission_id,
--        b.counted,
--        CASE WHEN b.counted >= b.target_count THEN 'not_received' ELSE 'in_progress' END,
--        CASE WHEN b.counted >= b.target_count THEN NOW() ELSE NULL END,
--        NOW(),
--        NOW()
--   FROM mp_backfill b
--  WHERE NOT EXISTS (SELECT 1 FROM mission_progress mp
--                     WHERE mp.user_id = b.user_id AND mp.mission_id = b.mission_id);
--
-- -- 3-2) 진행중인 기존 행을 정답까지 끌어올린다. 올리기만 하고 내리지 않는다.
-- UPDATE mission_progress mp
--   JOIN mp_backfill b
--     ON mp.user_id = b.user_id AND mp.mission_id = b.mission_id
--    SET mp.`count`      = b.counted,
--        mp.status       = CASE WHEN b.counted >= b.target_count THEN 'not_received' ELSE 'in_progress' END,
--        mp.completed_at = CASE WHEN b.counted >= b.target_count THEN COALESCE(mp.completed_at, NOW()) ELSE mp.completed_at END,
--        mp.updated_at   = NOW()
--  WHERE mp.status = 'in_progress'
--    AND b.counted > mp.`count`;
--
-- COMMIT;

DROP TEMPORARY TABLE IF EXISTS mp_backfill;
