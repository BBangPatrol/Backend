-- 미션 진행도 판정 규칙(criteria) 추가
-- 미션 타입만으로는 "누적 횟수 / 서로 다른 지역 수 / 서로 다른 빵집 수" 를 구분할 수 없다.
-- 값은 MissionCriteria enum 과 일치한다.

-- 백필을 위해 우선 NULL 허용으로 추가
ALTER TABLE mission ADD COLUMN criteria VARCHAR(40) NULL AFTER mission_type;

UPDATE mission SET criteria = 'RECEIPT_COUNT'   WHERE mission_type = 'receipt';
UPDATE mission SET criteria = 'REVIEW_COUNT'    WHERE mission_type = 'review';
UPDATE mission SET criteria = 'DISTINCT_BAKERY' WHERE mission_type = 'bakery';
UPDATE mission SET criteria = 'DISTINCT_ITEM'   WHERE mission_type = 'collection';

-- 5개 구에서 각각 1회 이상 = 누적 횟수가 아니라 서로 다른 지역 수
UPDATE mission SET criteria = 'DISTINCT_REGION' WHERE title = '전 지역 재패';

-- item 에 등급/캐릭터 컬럼이 없어 아직 판정 불가. 컬럼이 생기면 criteria 만 바꾸면 된다.
-- 조회 쿼리가 걸러내므로 그때까지 사용자에게 노출되지 않는다
UPDATE mission SET criteria = 'NOT_SUPPORTED'
 WHERE title IN ('고급 카드 획득하기', '희귀 카드 획득하기', '전설 카드 획득하기');

-- DEFAULT 를 두지 않아 신규 미션 등록 시 규칙 누락이 드러나게 한다
ALTER TABLE mission
    MODIFY COLUMN criteria VARCHAR(40) NOT NULL;

-- reward_point 가 NULL 이면 보상 수령 시 NPE 가 난다
UPDATE mission SET reward_point = 0 WHERE reward_point IS NULL;
ALTER TABLE mission
    MODIFY COLUMN reward_point INT NOT NULL DEFAULT 0;

-- 진행도는 (user, mission) 당 한 행. 첫 행동 시점에 생성하므로 중복 생성을 DB 에서 막는다
ALTER TABLE mission_progress
    ADD CONSTRAINT uk_mission_progress_user_mission UNIQUE (user_id, mission_id);

-- visits 도 (user, bakery) 당 한 행이다. 재인증 횟수는 visits.count 와 visit_detail 이 담당하므로
-- 이 제약이 같은 빵집 재인증을 막지는 않는다
ALTER TABLE visits
    ADD CONSTRAINT uk_visits_user_bakery UNIQUE (user_id, bakery_id);
