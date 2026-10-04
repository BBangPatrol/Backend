-- 시연용 빵집('꿈돌제과 시연점', business_number = '000-00-00000') 하드 삭제.
-- FK 에 ON DELETE CASCADE 가 없으므로 자식부터 순서대로 지운다.
-- [1] 사전 확인 → [2] 백업 → [3] 삭제 → [4] 검증 순으로 실행할 것.

-- =============================================================
-- [1] 사전 확인 (먼저 실행해서 결과를 눈으로 볼 것)
-- =============================================================
SET @b = (SELECT id FROM bakery WHERE business_number = '000-00-00000');
SELECT @b AS bakery_id, (SELECT name FROM bakery WHERE id = @b) AS name;

-- 시연 계정(test1~5@test.com, user1@test.com) 외의 사용자가 남긴 것이 있는지.
-- 0 이 아니면 실제 사용자 데이터가 함께 지워진다. 확인 후 진행할 것.
SELECT 'review' AS target, COUNT(*) AS rows_by_real_user
FROM review r JOIN `user` u ON u.id = r.user_id
WHERE r.bakery_id = @b AND u.email NOT IN
      ('test1@test.com','test2@test.com','test3@test.com','test4@test.com','test5@test.com','user1@test.com')
UNION ALL
SELECT 'visits', COUNT(*)
FROM visits v JOIN `user` u ON u.id = v.user_id
WHERE v.bakery_id = @b AND u.email NOT IN
      ('test1@test.com','test2@test.com','test3@test.com','test4@test.com','test5@test.com','user1@test.com')
UNION ALL
SELECT 'bookmark', COUNT(*) FROM bookmark WHERE bakery_id = @b
UNION ALL
SELECT 'review_like', COUNT(*)
FROM review_like rl JOIN review r ON r.id = rl.review_id WHERE r.bakery_id = @b;

-- 다른 가게의 리뷰가 이 가게의 방문 기록을 물고 있는지 (정상이면 0).
-- 0 이 아니면 아래 visit_detail 삭제가 FK 로 막히므로 원인부터 볼 것.
SELECT COUNT(*) AS cross_linked_reviews
FROM review r
         JOIN visit_detail vd ON vd.id = r.visit_detail_id
         JOIN visits v ON v.id = vd.visit_id
WHERE v.bakery_id = @b AND r.bakery_id <> @b;

-- =============================================================
-- [2] 백업 (셸에서 실행)
--
-- mysqldump -u <user> -p --single-transaction --no-create-info \
--   bbangpatrol bakery sig_image visits visit_detail review review_image review_keyword review_like bookmark \
--   > demo_bakery_backup_$(date +%Y%m%d).sql
-- =============================================================

-- =============================================================
-- [3] 삭제
-- =============================================================
START TRANSACTION;

SET @b = (SELECT id FROM bakery WHERE business_number = '000-00-00000');

DELETE rl FROM review_like rl JOIN review r ON r.id = rl.review_id WHERE r.bakery_id = @b;
DELETE rk FROM review_keyword rk JOIN review r ON r.id = rk.review_id WHERE r.bakery_id = @b;
DELETE ri FROM review_image ri JOIN review r ON r.id = ri.review_id WHERE r.bakery_id = @b;
DELETE FROM review WHERE bakery_id = @b;
DELETE vd FROM visit_detail vd JOIN visits v ON v.id = vd.visit_id WHERE v.bakery_id = @b;
DELETE FROM visits WHERE bakery_id = @b;
DELETE FROM sig_image WHERE bakery_id = @b;
DELETE FROM bookmark WHERE bakery_id = @b;
DELETE FROM bakery WHERE id = @b;

-- 결과를 보고 문제가 없으면 COMMIT, 이상하면 ROLLBACK
SELECT ROW_COUNT() AS deleted_bakery_rows;
COMMIT;

-- =============================================================
-- [4] 검증 (전부 0 이어야 한다)
-- =============================================================
SELECT (SELECT COUNT(*) FROM bakery WHERE business_number = '000-00-00000') AS bakery_left,
       (SELECT COUNT(*) FROM review WHERE bakery_id = @b)                   AS review_left,
       (SELECT COUNT(*) FROM visits WHERE bakery_id = @b)                   AS visits_left,
       (SELECT COUNT(*) FROM sig_image WHERE bakery_id = @b)                AS sig_image_left,
       (SELECT COUNT(*) FROM bookmark WHERE bakery_id = @b)                 AS bookmark_left;

-- =============================================================
-- [5] R2 오브젝트 정리 (demo/ 프리픽스 12개)
--   demo/bakery-signature.jpg, demo/bakery-signature_thumb.jpg
--   demo/review-1.jpg ~ demo/review-5.jpg 와 각 _thumb.jpg
-- =============================================================
