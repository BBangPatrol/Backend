-- =============================================================
-- 중복 영수증 조회(existsByReceiptHash / existsByReceiptHashAndUserId)가
-- visit_detail 풀스캔이었다. 유니크로 걸면 시연용으로 한 장을 여러 명에게
-- 돌릴 수 없으므로 일반 인덱스로 둔다.
--
-- V12 는 가맹점(지점 사업자번호) 작업이 쓰던 번호다. 그 작업이 보류된 동안에도
-- 이미 적용해 둔 DB 가 있어 번호를 비워두고 V13 으로 뒤에 붙인다.
-- 그 DB 에는 같은 이름의 인덱스가 이미 있으므로 존재 여부를 보고 만든다.
-- =============================================================
SET @exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'visit_detail'
      AND index_name = 'idx_visit_detail_receipt_hash'
);

SET @sql := IF(@exists > 0,
               'SELECT 1',
               'CREATE INDEX idx_visit_detail_receipt_hash ON visit_detail (receipt_hash)');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
