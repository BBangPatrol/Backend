-- =============================================================
-- schema_.sql에 추가한 created_at DEFAULT CURRENT_TIMESTAMP를
-- 기존 RDS 테이블에 반영하기 위한 1회성 ALTER 스크립트
-- (DROP/DATA 변경 없음, 컬럼 정의만 변경)
--
-- 2026-07-28 RDS(bbangpatrol-db)에 실행 완료.
--
-- 실행: EC2에서 mysql -h <RDS엔드포인트> -u <user> -p <db> < rds_alter_created_at_default.sql
-- =============================================================

ALTER TABLE user             MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE bakery          MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE item             MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mission           MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE review            MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE review_like        MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE bookmark           MODIFY created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mission_progress    MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE point_history       MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE visit_detail        MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
