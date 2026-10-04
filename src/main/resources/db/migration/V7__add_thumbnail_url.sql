-- =============================================================
-- 목록 조회용 썸네일 key 컬럼 추가
--
-- 그전에는 원본 key 에서 "_thumb.jpg" 규칙으로 파생했다. 규칙 방식은 썸네일이 없는 경우를
-- 표현할 수 없어서, 없으면 404 URL 을 그대로 내려보내야 했다.
-- 컬럼으로 두면 NULL 이 "썸네일 없음"을 뜻하고, 조회 시 서버가 원본으로 폴백할 수 있다.
--
-- image_url 과 같은 규칙: R2 오브젝트 키만 저장한다. public URL 변환은 R2Service.getPublicUrl 담당.
-- =============================================================
ALTER TABLE review_image ADD COLUMN thumbnail_url TEXT NULL AFTER image_url;
ALTER TABLE sig_image    ADD COLUMN thumbnail_url TEXT NULL AFTER image_url;

-- 이미 R2 에 만들어 둔 시그니처 메뉴 썸네일을 연결한다.
-- 시드 키가 bakeries/{id}/signature_menu.jpg 형태뿐이라 확장자만 바꿔주면 된다.
-- 규칙에 맞지 않는 키는 NULL 로 남겨 원본 폴백을 타게 한다.
UPDATE sig_image
   SET thumbnail_url = CONCAT(SUBSTRING(image_url, 1, CHAR_LENGTH(image_url) - 4), '_thumb.jpg')
 WHERE image_url LIKE '%.jpg'
   AND thumbnail_url IS NULL;

-- review_image 는 이 기능 도입 전 업로드된 행이 없어 채울 것이 없다.
-- 이후 업로드는 원본과 썸네일을 한 번에 올리며 두 컬럼을 함께 저장한다.
