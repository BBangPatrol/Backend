-- =============================================================
-- V17: 수집품 등급 미션 문구를 화면 표기에 맞춘다 (희귀 / 영웅 / 전설)
--
-- V10 에서 '커먼 / 레어 / 에픽 / 레전더리'로 통일했는데, 정작 사용자가 보는 쪽은 다른 말을 쓴다.
--   - 도감 배지(Frontend COLLECTIBLE_RANK_STYLES): 일반 / 희귀 / 영웅 / 전설
--   - 수집품 기획서: 일반 / 희귀 / 영웅 / 전설
-- 그래서 배지에서 '영웅'을 받은 사람이 미션 목록에서는 '에픽 카드 획득하기'를 찾게 된다.
-- 표기를 화면 쪽으로 맞춘다. V10 의 방향을 뒤집는 것이다.
--
-- 바뀌는 것은 문구(title, description)뿐이다.
-- 등급 값(item.rank = NORMAL/RARE/EPIC/LEGENDARY)과 판정 규칙(criteria)은 그대로다.
-- 등급 값은 클라이언트에 enum 이름으로 나가고 배지가 한글 라벨을 붙이므로 건드릴 이유가 없다.
--
-- V11 이 단계별 미션(3개/5개/10개)을 더 넣어 등급당 행이 여러 개다.
-- 그래서 title 로 찾지 않고 criteria 로 찾아 REPLACE 한다. 문구가 늘어도 이 방식이면 함께 바뀐다.
-- =============================================================
UPDATE mission
   SET title       = REPLACE(title, '레어', '희귀'),
       description = REPLACE(description, '레어', '희귀')
 WHERE criteria = 'DISTINCT_ITEM_RARE';

UPDATE mission
   SET title       = REPLACE(title, '에픽', '영웅'),
       description = REPLACE(description, '에픽', '영웅')
 WHERE criteria = 'DISTINCT_ITEM_EPIC';

UPDATE mission
   SET title       = REPLACE(title, '레전더리', '전설'),
       description = REPLACE(description, '레전더리', '전설')
 WHERE criteria = 'DISTINCT_ITEM_LEGENDARY';
