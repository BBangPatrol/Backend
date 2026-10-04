-- =============================================================
-- V10: 수집품 등급 명칭 통일 (커먼 / 레어 / 에픽 / 레전더리)
--
-- 같은 등급을 두 이름으로 부르고 있었다.
--   EPIC      -> 미션 문구는 '고급', ItemRank 주석은 '영웅'
--   RARE      -> '희귀'
--   LEGENDARY -> '전설'
--
-- 클라이언트에는 rank 가 enum 이름(NORMAL/RARE/EPIC/LEGENDARY)으로 그대로 나가므로
-- 한글 명칭이 남아 있는 곳은 미션 문구뿐이다. 여기를 통일한다.
--
-- title 이 아니라 criteria 로 찾는 이유: title 은 지금 바꾸는 대상이라 기준으로 쓰면
-- 이 파일 안에서 실행 순서에 묶인다. criteria 는 V5 가 채워 둔 값이고 등급당 한 행뿐이다.
--
-- 커먼(NORMAL) 등급 미션은 없어서 바꿀 것이 없다.
-- =============================================================
UPDATE mission
   SET title       = '레어 카드 획득하기',
       description = '레어 등급 수집품 1개 획득하기'
 WHERE criteria = 'DISTINCT_ITEM_RARE';

UPDATE mission
   SET title       = '에픽 카드 획득하기',
       description = '에픽 등급 수집품 1개 획득하기'
 WHERE criteria = 'DISTINCT_ITEM_EPIC';

UPDATE mission
   SET title       = '레전더리 카드 획득하기',
       description = '레전더리 등급 수집품 1개 획득하기'
 WHERE criteria = 'DISTINCT_ITEM_LEGENDARY';
