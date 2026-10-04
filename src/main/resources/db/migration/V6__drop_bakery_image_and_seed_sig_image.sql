-- =============================================================
-- bakery_image 제거
-- 빵집 이미지는 시그니처 메뉴 사진 한 장(sig_image)으로 통합한다.
-- R2에 bakeries/{bakery_id}/signature_menu.jpg 형태로만 올라가 있어
-- 별도의 가게 외관 이미지 원본이 존재하지 않는다.
-- =============================================================
DROP TABLE IF EXISTS bakery_image;

-- =============================================================
-- sig_image 시드
-- image_url 에는 R2 오브젝트 키만 저장한다.
-- public URL 변환은 조회 시 R2Service.getPublicUrl 이 담당한다. (item 시드와 동일한 규칙)
-- =============================================================
INSERT INTO sig_image (image_url, bakery_id)
SELECT CONCAT('bakeries/', b.id, '/signature_menu.jpg'), b.id
FROM bakery b
WHERE b.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sig_image s WHERE s.bakery_id = b.id);
