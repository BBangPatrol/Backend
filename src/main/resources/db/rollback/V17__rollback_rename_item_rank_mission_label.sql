-- =============================================================
-- V17 되돌리기: 수집품 등급 미션 문구를 V10 표기(레어 / 에픽 / 레전더리)로 되돌린다.
-- 문구만 되돌리며, 등급 값과 criteria 는 애초에 건드리지 않았으므로 복구할 것이 없다.
-- =============================================================
UPDATE mission
   SET title       = REPLACE(title, '희귀', '레어'),
       description = REPLACE(description, '희귀', '레어')
 WHERE criteria = 'DISTINCT_ITEM_RARE';

UPDATE mission
   SET title       = REPLACE(title, '영웅', '에픽'),
       description = REPLACE(description, '영웅', '에픽')
 WHERE criteria = 'DISTINCT_ITEM_EPIC';

UPDATE mission
   SET title       = REPLACE(title, '전설', '레전더리'),
       description = REPLACE(description, '전설', '레전더리')
 WHERE criteria = 'DISTINCT_ITEM_LEGENDARY';
