-- 컨테이너 타임존을 KST 로 바꾸기 전에 쌓인 UTC 시각을 +9시간 보정한다.
-- DATE 컬럼(visit_detail.visited_at, mission.start_date/end_date)은 영수증 날짜·미션 기간이라 건드리지 않는다.
--
-- 한 번만 실행할 것. 두 번 돌리면 18시간이 더해진다.
-- 실행 전 백업:
--   mysqldump -u <user> -p --single-transaction bbangpatrol > before_tz_shift_$(date +%Y%m%d).sql

START TRANSACTION;

UPDATE `user`
SET created_at = created_at + INTERVAL 9 HOUR,
    deleted_at = deleted_at + INTERVAL 9 HOUR;

UPDATE bakery
SET created_at = created_at + INTERVAL 9 HOUR,
    updated_at = updated_at + INTERVAL 9 HOUR,
    deleted_at = deleted_at + INTERVAL 9 HOUR;

UPDATE item SET created_at = created_at + INTERVAL 9 HOUR;

UPDATE mission SET created_at = created_at + INTERVAL 9 HOUR;

UPDATE review
SET created_at = created_at + INTERVAL 9 HOUR,
    deleted_at = deleted_at + INTERVAL 9 HOUR;

UPDATE review_like SET created_at = created_at + INTERVAL 9 HOUR;

UPDATE bookmark SET created_at = created_at + INTERVAL 9 HOUR;

UPDATE user_item SET acquired_at = acquired_at + INTERVAL 9 HOUR;

UPDATE mission_progress
SET created_at   = created_at + INTERVAL 9 HOUR,
    updated_at   = updated_at + INTERVAL 9 HOUR,
    completed_at = completed_at + INTERVAL 9 HOUR;

UPDATE point_history SET created_at = created_at + INTERVAL 9 HOUR;

UPDATE visit_detail SET created_at = created_at + INTERVAL 9 HOUR;

-- 최근 행 몇 개를 눈으로 확인한 뒤 COMMIT, 이상하면 ROLLBACK
SELECT id, content, amount, created_at FROM point_history ORDER BY id DESC LIMIT 5;
SELECT id, user_id, item_id, acquired_at FROM user_item ORDER BY id DESC LIMIT 5;

COMMIT;
