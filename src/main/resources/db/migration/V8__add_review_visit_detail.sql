-- =============================================================
-- 리뷰를 영수증 인증 방문 기록(visit_detail)에 연결
--
-- NULL 을 허용하는 이유: 이 기능 도입 전 리뷰는 짝지을 방문 기록이 없고 백필도 불가능하다.
-- "리뷰를 쓰려면 인증이 필요하다" 는 ReviewService 가 신규 작성 경로에서만 강제한다.
-- MySQL 의 UNIQUE 는 NULL 을 중복으로 보지 않아 기존 행들과 공존한다.
-- =============================================================
ALTER TABLE review
    ADD COLUMN visit_detail_id BIGINT NULL AFTER bakery_id,
    ADD CONSTRAINT uk_review_visit_detail UNIQUE (visit_detail_id),
    ADD CONSTRAINT fk_review_visit_detail
        FOREIGN KEY (visit_detail_id) REFERENCES visit_detail (id);
