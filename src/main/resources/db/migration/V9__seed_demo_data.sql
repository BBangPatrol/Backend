-- =============================================================
-- V9: 시연용 데이터 (빵집 1곳 / 시그니처 사진 / 리뷰 6건)
--
-- 실재하지 않는 가짜 가게다. 이름에 "시연점"을 넣고 사업자번호를 000-00-00000 으로 둬서
-- 화면에서도, DB 에서도 시연용임이 드러나게 했다. 지울 때는 파일 맨 아래 정리 SQL 을 쓸 것.
--
-- 이 파일은 배포 시 운영 DB 에도 적용된다. 시연이 끝나면 정리 SQL 로 지우는 것을 전제로 한다.
--
-- 사진은 R2 의 demo/ 프리픽스에 올려두었다. bakeries/{id}/... 규칙(V6)을 따르지 않은 이유는
-- bakery.id 가 AUTO_INCREMENT 라 이 파일을 쓰는 시점에 확정되지 않기 때문이다.
-- image_url 에는 R2 오브젝트 키만 저장한다 (public URL 변환은 R2Service.getPublicUrl 담당).
--
-- 리뷰는 visit_detail(영수증 인증) 한 건에 1:1 로 묶인다 (V8). 그래서 리뷰마다
-- visits / visit_detail 을 함께 만든다. 작성자는 V2 가 넣어둔 더미 유저 6명이다.
--
-- AUTO_INCREMENT 로 정해지는 id 는 LAST_INSERT_ID() 로 받아 사용자 변수에 담는다.
-- Flyway 는 마이그레이션 하나를 커넥션 하나에서 실행하므로 변수가 문장 사이에서 유지된다.
-- =============================================================

-- =============================================================
-- 빵집
-- 위치는 시연 장소(SSAFY 대전캠퍼스) 근처로 잡았다. 거리순 정렬에서 바로 위에 올라온다.
-- =============================================================
INSERT INTO `bakery`
(`name`, `region`, `address`, `lat`, `lng`, `phone`, `hours`, `business_number`, `signature_menu`, `content`, `summary`)
VALUES
    ('꿈돌제과 시연점', '유성구', '대전광역시 유성구 동서대로 98-39, 1층', 36.3547000, 127.2985000,
     '042-000-0000', '10:00-20:00, 일 휴무', '000-00-00000',
     '꿈돌이 소금빵, 명인 단팥빵, 버터 크루아상',
     '시연을 위해 만든 가상의 빵집이다. 실제로 존재하지 않는다. 매일 아침 구운 식사빵과 구움과자를 판매한다는 설정으로, 시그니처인 ''꿈돌이 소금빵''은 겉이 바삭하고 속이 촉촉하다. 국산 팥을 직접 끓여 넣은 ''명인 단팥빵''과 결이 살아있는 ''버터 크루아상''도 함께 준비돼 있다. 리뷰·사진·방문 인증 화면을 채우기 위한 데이터이므로 시연이 끝나면 정리한다.',
     '시연용 가상 빵집. 실제로 존재하지 않는다.');

SET @bakery_id = LAST_INSERT_ID();

-- 시그니처 메뉴 사진 (V6 이후 빵집 사진은 sig_image 한 장으로 통합돼 있다)
INSERT INTO sig_image (origin, image_url, thumbnail_url, bakery_id)
VALUES ('demo-bakery-signature.jpg', 'demo/bakery-signature.jpg', 'demo/bakery-signature_thumb.jpg', @bakery_id);

-- =============================================================
-- 작성자 (V2 더미 유저)
-- 이 유저들이 지워진 DB 에서는 아래 INSERT 가 user_id NOT NULL 로 실패한다.
-- =============================================================
SET @u1 = (SELECT id FROM `user` WHERE email = 'test1@test.com');  -- 황우찬
SET @u2 = (SELECT id FROM `user` WHERE email = 'test2@test.com');  -- 김현호
SET @u3 = (SELECT id FROM `user` WHERE email = 'test3@test.com');  -- 박성수
SET @u4 = (SELECT id FROM `user` WHERE email = 'test4@test.com');  -- 박승진
SET @u5 = (SELECT id FROM `user` WHERE email = 'test5@test.com');  -- 오은진
SET @u6 = (SELECT id FROM `user` WHERE email = 'user1@test.com');  -- 일반사용자

-- =============================================================
-- 방문 인증 + 리뷰
--
-- receipt_hash 는 원래 ReceiptHashService 가 사업자번호:승인번호:날짜:금액 을 SHA-256 한 값이다.
-- 시연 데이터는 실제 영수증이 없으므로 겹치지 않을 문자열을 같은 방식으로 해싱해서 넣는다.
-- (existsByReceiptHash 중복 검사에 걸리지 않으면 되고, 값 자체를 되돌려 쓰는 코드는 없다.)
-- visits 는 (user, bakery) 당 한 행이고 재방문 횟수는 count 가 담당한다 (V3 의 uk_visits_user_bakery).
-- =============================================================

-- 리뷰 1 — 황우찬 (사진 2장)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u1);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (12800, '2026-09-04', SHA2('demo:kkumdol:0004', 256), '2026-09-04 15:12:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (5, '소금빵 겉이 바삭하고 속은 촉촉해요. 우유 식빵도 같이 샀는데 둘 다 만족했습니다.',
        4, '2026-09-04 16:40:00', @u1, @bakery_id, @vd);
SET @r1 = LAST_INSERT_ID();

INSERT INTO review_image (origin, image_url, thumbnail_url, review_id) VALUES
    ('demo-review-1.jpg', 'demo/review-1.jpg', 'demo/review-1_thumb.jpg', @r1),
    ('demo-review-4.jpg', 'demo/review-4.jpg', 'demo/review-4_thumb.jpg', @r1);

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r1, id FROM keyword WHERE label IN ('디저트 맛집', '가성비 굿');

-- 리뷰 2 — 김현호 (사진 1장)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u2);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (8500, '2026-09-05', SHA2('demo:kkumdol:0005', 256), '2026-09-05 11:30:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (4, '단팥빵 팥이 안 달아서 좋았어요. 종류가 많아서 고르는 재미가 있습니다.',
        2, '2026-09-05 12:05:00', @u2, @bakery_id, @vd);
SET @r2 = LAST_INSERT_ID();

INSERT INTO review_image (origin, image_url, thumbnail_url, review_id)
VALUES ('demo-review-2.jpg', 'demo/review-2.jpg', 'demo/review-2_thumb.jpg', @r2);

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r2, id FROM keyword WHERE label IN ('메뉴 다양', '가성비 굿');

-- 리뷰 3 — 박성수 (사진 1장)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u3);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (15200, '2026-09-06', SHA2('demo:kkumdol:0006', 256), '2026-09-06 10:20:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (5, '크루아상 결이 살아있어요. 버터 향이 진해서 커피랑 같이 먹기 좋습니다.',
        5, '2026-09-06 13:15:00', @u3, @bakery_id, @vd);
SET @r3 = LAST_INSERT_ID();

INSERT INTO review_image (origin, image_url, thumbnail_url, review_id)
VALUES ('demo-review-3.jpg', 'demo/review-3.jpg', 'demo/review-3_thumb.jpg', @r3);

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r3, id FROM keyword WHERE label IN ('커피 맛집', '디저트 맛집');

-- 리뷰 4 — 박승진 (사진 없음)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u4);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (6300, '2026-09-08', SHA2('demo:kkumdol:0008', 256), '2026-09-08 17:45:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (4, '주차가 편해서 좋았어요. 인기 메뉴는 일찍 품절이라 오전에 가는 걸 추천합니다.',
        1, '2026-09-08 19:00:00', @u4, @bakery_id, @vd);
SET @r4 = LAST_INSERT_ID();

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r4, id FROM keyword WHERE label IN ('주차 편함', '넓고 쾌적');

-- 리뷰 5 — 오은진 (사진 1장)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u5);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (31000, '2026-09-10', SHA2('demo:kkumdol:0010', 256), '2026-09-10 14:00:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (5, '생크림 케이크가 안 느끼하고 딱 좋아요. 가게도 넓고 조용해서 앉아있기 편했습니다.',
        3, '2026-09-10 15:30:00', @u5, @bakery_id, @vd);
SET @r5 = LAST_INSERT_ID();

INSERT INTO review_image (origin, image_url, thumbnail_url, review_id)
VALUES ('demo-review-5.jpg', 'demo/review-5.jpg', 'demo/review-5_thumb.jpg', @r5);

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r5, id FROM keyword WHERE label IN ('감성 인테리어', '조용함', '넓고 쾌적');

-- 리뷰 6 — 일반사용자 (사진 없음, 좋아요 0)
INSERT INTO visits (count, bakery_id, user_id) VALUES (1, @bakery_id, @u6);
SET @v = LAST_INSERT_ID();
INSERT INTO visit_detail (total_amount, visited_at, receipt_hash, created_at, visit_id)
VALUES (4200, '2026-09-11', SHA2('demo:kkumdol:0011', 256), '2026-09-11 18:10:00', @v);
SET @vd = LAST_INSERT_ID();

INSERT INTO review (rating, content, like_count, created_at, user_id, bakery_id, visit_detail_id)
VALUES (3, '맛은 괜찮은데 사람이 많아 대기가 길었어요. 자리는 넉넉한 편입니다.',
        0, '2026-09-11 18:50:00', @u6, @bakery_id, @vd);
SET @r6 = LAST_INSERT_ID();

INSERT INTO review_keyword (review_id, keyword_id)
SELECT @r6, id FROM keyword WHERE label IN ('메뉴 다양');

-- =============================================================
-- 좋아요
-- like_count 는 review 행에 저장된 값이고 토글 시 ±1 로만 움직인다.
-- 여기서 행 수를 like_count 와 맞춰두지 않으면 시연 중에 좋아요를 눌렀을 때 숫자가 어긋난다.
-- =============================================================
INSERT INTO review_like (created_at, user_id, review_id) VALUES
    ('2026-09-04 18:00:00', @u2, @r1),
    ('2026-09-05 09:10:00', @u3, @r1),
    ('2026-09-05 20:30:00', @u4, @r1),
    ('2026-09-06 08:40:00', @u5, @r1),

    ('2026-09-05 13:20:00', @u1, @r2),
    ('2026-09-06 09:00:00', @u3, @r2),

    ('2026-09-06 14:00:00', @u1, @r3),
    ('2026-09-06 15:10:00', @u2, @r3),
    ('2026-09-07 10:05:00', @u4, @r3),
    ('2026-09-07 11:20:00', @u5, @r3),
    ('2026-09-08 09:30:00', @u6, @r3),

    ('2026-09-09 08:15:00', @u1, @r4),

    ('2026-09-10 16:00:00', @u1, @r5),
    ('2026-09-10 17:25:00', @u2, @r5),
    ('2026-09-11 09:40:00', @u6, @r5);

-- =============================================================
-- 평점
-- avg_rating 은 리뷰에서 계산되지 않고 bakery 행에 저장된 값을 그대로 내려준다.
-- 위 6건의 평균 (5+4+5+4+5+3)/6 = 4.33 을 DECIMAL(2,1) 에 맞춰 반올림했다.
-- =============================================================
UPDATE bakery SET avg_rating = 4.3, updated_at = NOW() WHERE id = @bakery_id;

-- =============================================================
-- 시연이 끝난 뒤 정리 (Flyway 는 되돌려주지 않으므로 직접 실행할 것)
-- R2 의 demo/ 프리픽스 오브젝트 12개도 함께 지우면 된다.
--
-- SET @b = (SELECT id FROM bakery WHERE business_number = '000-00-00000');
-- DELETE rl FROM review_like rl JOIN review r ON r.id = rl.review_id WHERE r.bakery_id = @b;
-- DELETE rk FROM review_keyword rk JOIN review r ON r.id = rk.review_id WHERE r.bakery_id = @b;
-- DELETE ri FROM review_image ri JOIN review r ON r.id = ri.review_id WHERE r.bakery_id = @b;
-- DELETE FROM review WHERE bakery_id = @b;
-- DELETE vd FROM visit_detail vd JOIN visits v ON v.id = vd.visit_id WHERE v.bakery_id = @b;
-- DELETE FROM visits WHERE bakery_id = @b;
-- DELETE FROM sig_image WHERE bakery_id = @b;
-- DELETE FROM bakery WHERE id = @b;
-- =============================================================
