-- =============================================================
-- bakery_business_number
-- 빵산책 지도는 브랜드당 한 매장만 싣는다. 같은 브랜드의 다른 지점은
-- 사업자번호가 달라 영수증 인증이 막히므로, 한 매장으로 인정할
-- 사업자번호를 여기에 늘려 둔다. bakery.business_number 는 대표번호로 남긴다
-- =============================================================
CREATE TABLE bakery_business_number (
                                        id              BIGINT       NOT NULL AUTO_INCREMENT,
                                        bakery_id       BIGINT       NOT NULL,
                                        business_number VARCHAR(30)  NOT NULL,
                                        branch_name     VARCHAR(100) NULL,
                                        -- 지점이 위치한 구. 미션 진행도를 실제 방문한 구로 집계하기 위해 둔다.
                                        -- NULL 이면 bakery.region 을 따른다
                                        region          VARCHAR(20)  NULL,
                                        created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (id),
    -- 두 빵집이 같은 번호를 가지면 엉뚱한 가게로 인증이 통과한다
                                        UNIQUE KEY uk_bakery_business_number (business_number),
                                        KEY idx_bakery_business_number_bakery (bakery_id),
                                        CONSTRAINT fk_bakery_business_number_bakery
                                            FOREIGN KEY (bakery_id) REFERENCES bakery (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- 방문 기록에 "실제로 어디서 샀는지" 를 남긴다.
--
-- region 이 필요한 이유: 구별 미션 집계가 visit.bakery.region 을 타고 있어서
-- 중구 지점에서 끊은 영수증이 지도에 실린 대덕구 미션으로 잡힌다.
-- 방문 시점의 구를 여기에 박아두고 그걸로 센다.
--
-- receipt_business_number 는 나중에 지점을 별도 빵집으로 분리하게 될 때
-- 기존 방문 기록을 쪼갤 근거가 된다
-- =============================================================
ALTER TABLE visit_detail
    ADD COLUMN region                  VARCHAR(20) NULL AFTER receipt_hash,
    ADD COLUMN receipt_business_number VARCHAR(30) NULL AFTER region;

-- 기존 방문은 전부 지도에 실린 매장에서 산 것이므로 빵집의 구로 채운다
UPDATE visit_detail vd
    JOIN visits v ON v.id = vd.visit_id
    JOIN bakery b ON b.id = v.bakery_id
SET vd.region = b.region
WHERE vd.region IS NULL;

-- 중복 영수증 조회가 풀스캔이었다. 유니크로 걸면 시연용 공유 영수증이 막히므로 일반 인덱스로 둔다
CREATE INDEX idx_visit_detail_receipt_hash ON visit_detail (receipt_hash);

-- 확보한 실물 영수증 기준 지점 등록
INSERT INTO bakery_business_number (bakery_id, business_number, branch_name, region)
SELECT b.id, '816-86-02784', '중교로점', '중구'
FROM bakery b
WHERE b.name = '몽심'
LIMIT 1;

INSERT INTO bakery_business_number (bakery_id, business_number, branch_name, region)
SELECT b.id, '644-40-01412', '시청점', '서구'
FROM bakery b
WHERE b.name = '콜드버터베이크샵'
LIMIT 1;
