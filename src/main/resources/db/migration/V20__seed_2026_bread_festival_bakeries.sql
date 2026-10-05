-- =============================================================
-- V20: 2026 대전빵축제 참여 빵집 120곳 반영
--
-- 2026 대전빵축제(10.17~18, 엑스포과학공원 한빛탑 일원, 주최 대전관광공사) 참여 빵집 120곳 중
--   - 이미 있는 29곳: content 끝에 참가 안내 한 문장을 덧붙인다.
--   - 없던 91곳: 새로 넣는다.
--
-- 명단 출처: 대전관광공사 보도자료 기반 기사(국제뉴스 A·B존, 위키트리, 비건뉴스)와
-- 공식 배치도 전사본(make2t, trvlogue)을 교차 대조했다. 공식 홈페이지에는 명단이 없고
-- 인스타그램(@bakery_festival_daejeon) 이미지로만 공개돼 있어 텍스트 소스는 이것이 전부다.
-- 주소는 카카오 장소(다음 통합검색 장소 탭) 78곳 + 다이닝코드 등 공개 페이지 11곳에서 가져왔고,
-- 지점이 여러 개면 본점 주소를 썼다(비비스 미트파이 오븐·폴레폴레·도안양과점은 지점 → 본점으로 교체.
-- 안가네대추빵은 본점이 충북 음성이라 대전점 유지). 검증 기록은 bakery_export/bread_festival_2026_VERIFICATION.md.
--
-- 위도·경도는 V2 와 같이 카카오 로컬 API(주소 검색)로 받았다 (2026-10-05 조회, 도로명+번지 기준).
--
-- 채우지 못한 것:
--   - hours / business_number / avg_rating: 공개 텍스트에 없음.
--   - 촉촉디저트 잼살롱(선화동), 이프리(복용동) 2곳은 지도 서비스 어디에도 등록이 없어 동 단위 주소만 넣었고
--     좌표도 NULL 이다. 영수증 인증은 도로명이 없으면 구 단위 대조로 넘어가므로 막히지는 않지만,
--     상세 주소를 확인하면 주소·좌표를 UPDATE 할 것.
--   - 축제 부스 번호/존은 매장 정보가 아니라 넣지 않았다.
--
-- id 가 아니라 name 을 조건으로 쓴다. 다른 환경에서 이미 반영돼 있으면
-- UPDATE 는 content LIKE 조건에, INSERT 는 NOT EXISTS 조건에 걸려 아무 것도 바꾸지 않는다.
-- =============================================================

-- -------------------------------------------------------------
-- 1. 이미 있는 29곳: content 끝에 참가 안내 추가
-- -------------------------------------------------------------
SET @fest = '2026 대전빵축제(10월 17일~18일, 엑스포과학공원 한빛탑 일원) 참여 빵집입니다!';

UPDATE bakery
   SET content = CONCAT(IF(content IS NULL OR content = '', '', CONCAT(content, '\n\n')), @fest)
 WHERE name IN (
        '보보로베이커리',
        '에코브레드하우스',
        '가또앤브레드',
        '오늘의빵집',
        '언니네빵집',
        '요이그',
        '버터포인트',
        '오븐브라더스',
        '다다제과점',
        '케이크퍼즐',
        '내가 잘가는 빵집',
        '캘리포니아베이커리',
        '몰랑몰랑',
        '빵앗간',
        '손수베이커리',
        '오픈오븐',
        '연선흠베이커리',
        '슬로우브레드',
        '콜마르브레드',
        '파티세리소신',
        '써틴브레드',
        '토우베이크하우스',
        '파이코코',
        '한스브레드',
        '성심당',
        '빵, 한모금',
        '연이가 베이크샵',
        '하레하레',
        '다소리과자점'
       )
   AND (content IS NULL OR content NOT LIKE '%2026 대전빵축제%');

-- -------------------------------------------------------------
-- 2. 없던 91곳 추가 (이름이 이미 있으면 건너뜀)
-- -------------------------------------------------------------

-- 서구 (38곳)
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '달달보드레', '서구', '대전광역시 서구 복수남로 35 1층', 36.2979761, 127.3775681, '042-583-4883',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '달달보드레');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '마마 러브 파이', '서구', '대전광역시 서구 둔산남로 3 운암키즈몰 1층 109호', 36.3481010, 127.3781290, '042-477-1380',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '마마 러브 파이');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '앙크', '서구', '대전광역시 서구 둔산로218번길 30 까사보니타 1동 1층 101호', 36.3498651, 127.3980751, '0502-5554-1466',
       '무설탕·저탄수 당근치즈케이크, 머핀 3종 세트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '앙크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '글로우', '서구', '대전광역시 서구 구봉산북로21번길 64-16', 36.2969044, 127.3256254, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '글로우');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '에그:휘', '서구', '대전광역시 서구 계룡로649번길 47', 36.3394104, 127.3956265, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '에그:휘');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '버터컵', '서구', '대전광역시 서구 월평중로13번길 9-9 1층', 36.3552923, 127.3635043, NULL,
       '카이막소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '버터컵');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '필라델피아 호기 스쿨', '서구', '대전광역시 서구 남선로 49', 36.3476375, 127.3975387, NULL,
       '오리지날 호기, 스파이시 호기',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '필라델피아 호기 스쿨');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '조아유즈', '서구', '대전광역시 서구 용소로43번길 33 101호', 36.3106926, 127.3519445, '010-2561-8641',
       '바나피타르트, 블루베리치즈타르트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '조아유즈');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '베이커리설래', '서구', '대전광역시 서구 둔지로 46 갤러리빌 1층 106호', 36.3537247, 127.3768675, '0503-7153-4959',
       '미니 졸라꿀 치아바타, 미니 감자 캄파뉴',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '베이커리설래');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '블라썸케이크', '서구', '대전광역시 서구 도산로341번길 96 롯데드림빌 1층 102호', 36.3389302, 127.3863092, '010-6738-1052',
       '두바이 쫀득쿠키, 피자소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '블라썸케이크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '앙금엉금케이크', '서구', '대전광역시 서구 용소로43번길 17 은빌라 1층', 36.3099155, 127.3520202, '0507-1366-7118',
       '케이컵, 카스테라크림빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '앙금엉금케이크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '원두집', '서구', '대전광역시 서구 구봉산북로21번길 12 1층', 36.2952881, 127.3239471, '010-2126-0502',
       '버터바, 커피 판나코타',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '원두집');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '커피노리', '서구', '대전광역시 서구 둔산중로 38 105호', 36.3499634, 127.3872454, '042-483-5651',
       '버터카라멜샌드쿠키, 고메바닐라에그타르트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '커피노리');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '앤크', '서구', '대전광역시 서구 청사서로 14 대성빌딩 1층', 36.3591448, 127.3768851, '0502-5551-9928',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '앤크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '카페이이오', '서구', '대전광역시 서구 용소로39번길 32 1층 102호', 36.3104028, 127.3517539, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '카페이이오');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '썸띵라이크', '서구', '대전광역시 서구 계룡로491번길 8 1층 107호', 36.3455375, 127.3802514, '010-4204-1993',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '썸띵라이크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '빨간다방', '서구', '대전광역시 서구 계백로860번길 58 1층', 36.2954352, 127.3248572, '042-380-9010',
       '탕종 빨간식빵, 탕종 빨간 소금식빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '빨간다방');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '브루블룸', '서구', '대전광역시 서구 둔지로 52 갤러리빌7차 1층 101호', 36.3542681, 127.3769043, '0507-1327-4138',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '브루블룸');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '도안양과점', '서구', '대전광역시 서구 원도안로179번길 72 1층 102호', 36.3185850, 127.3440750, NULL,
       '블루베리 요거트 퀸아망, 순우유 크림 퀸아망',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '도안양과점');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '목화제과', '서구', '대전광역시 서구 도산로393번길 13 1층', 36.3392152, 127.3932524, '070-7818-0107',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '목화제과');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '다르코봉봉', '서구', '대전광역시 서구 원도안로 13-24 1층', 36.3048089, 127.3489738, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '다르코봉봉');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '베이커리 미르', '서구', '대전광역시 서구 계룡로603번길 22 1층 103호', 36.3414486, 127.3912049, '042-535-0045',
       '초코시나몬롤, 화이트시나몬롤',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '베이커리 미르');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '젤리포에', '서구', '대전광역시 서구 계룡로 399-1 1층 101호', 36.3511886, 127.3724406, '010-2586-0201',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '젤리포에');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '빵순상점', '서구', '대전광역시 서구 가장로 66 2층', 36.3365403, 127.3822386, '010-6509-0400',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '빵순상점');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '카페비포유', '서구', '대전광역시 서구 도산로 251 성보빌딩 1층', 36.3300395, 127.3825201, '010-2467-8136',
       '꿈돌이마카롱, 우리쌀에그타르트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '카페비포유');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '쿠피', '서구', '대전광역시 서구 원도안로241번길 14-31 1층', 36.3262028, 127.3465329, '042-710-7754',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '쿠피');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '명지제과', '서구', '대전광역시 서구 정림로24번길 30', 36.3027549, 127.3678851, '042-585-9255',
       '밤치즈케이크산도, 샤인머스켓산도',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '명지제과');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '진커피', '서구', '대전광역시 서구 월평중로 13 1층', 36.3557900, 127.3637513, '042-482-0507',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '진커피');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '킴스베이크하우스', '서구', '대전광역시 서구 둔산중로 70 1층', 36.3528670, 127.3871137, '0507-1385-0280',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '킴스베이크하우스');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '정담', '서구', '대전광역시 서구 문정로112번안길 40 1층', 36.3456903, 127.3937862, NULL,
       '칠리스치즈소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '정담');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '파티세리 러츠', '서구', '대전광역시 서구 둔산로74번길 12 1층', 36.3507365, 127.3816965, '010-7568-0219',
       '유잼대전, 홍차에유',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '파티세리 러츠');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '하디앤소프티', '서구', '대전광역시 서구 도안북로93번길 31 도안더블루힐 1층 108~109호', 36.3315071, 127.3386487, '070-7640-3375',
       '사워도우 샌드위치, 카라멜 피칸 버터 바게트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '하디앤소프티');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '윤달', '서구', '대전광역시 서구 도안대로 55 1층 101호', 36.3066482, 127.3343841, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '윤달');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '클로버 포 유', '서구', '대전광역시 서구 도산로253번길 45 1층', 36.3313567, 127.3808306, '0503-7151-5297',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '클로버 포 유');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '모모데이', '서구', '대전광역시 서구 도안동로11번길 33 1층', 36.3055869, 127.3511830, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '모모데이');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '카페루엘', '서구', '대전광역시 서구 복수남로36번길 27 1층', 36.2997796, 127.3774705, '070-4320-7826',
       '복수동 맛피아의 밤티라미수, 스리라차마요 소금빵 샌드위치',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '카페루엘');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '굿모닝마들렌', '서구', '대전광역시 서구 갈마역로25번길 27-5 1층 102호', 36.3525953, 127.3727935, '010-8178-1259',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '굿모닝마들렌');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '텀하프트', '서구', '대전광역시 서구 용소로 41 1층', 36.3092842, 127.3522661, '010-4526-8774',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '텀하프트');

-- 대덕구 (7곳)
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '쿠키랜드', '대덕구', '대전광역시 대덕구 계족산로43번길 51 104호', 36.3646425, 127.4354897, '070-8834-5756',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '쿠키랜드');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '달곰아저씨', '대덕구', '대전광역시 대덕구 대덕대로 1586 1층', 36.4501276, 127.4239752, '070-7525-0114',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '달곰아저씨');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '카페누룽지', '대덕구', '대전광역시 대덕구 덕암북로 65 1층', 36.4449884, 127.4245194, NULL,
       '크림치즈찹쌀떡누룽지, 누룽지소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '카페누룽지');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '신라당', '대덕구', '대전광역시 대덕구 계족로663번길 29-18', 36.3696171, 127.4269489, '042-622-1464',
       '밤식빵, 단팥빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '신라당');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT 'NTBR', '대덕구', '대전광역시 대덕구 신탄진로36번길 121', 36.3891694, 127.4280201, '010-3432-6737',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = 'NTBR');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '이든베이커리', '대덕구', '대전광역시 대덕구 신탄진로218번길 42', 36.4009982, 127.4241139, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '이든베이커리');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '코코노빵야', '대덕구', '대전광역시 대덕구 신탄진동로48번길 6', 36.4478905, 127.4345483, NULL,
       '불닭감자소금빵, 콘치즈소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '코코노빵야');

-- 유성구 (24곳)
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '르뺑99-1', '유성구', '대전광역시 유성구 온천북로33번길 22-3 101호', 36.3579104, 127.3472899, '0507-1416-9914',
       '명란바게트, 초코소라빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '르뺑99-1');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '폴드 앤 베이크', '유성구', '대전광역시 유성구 구즉로48번길 10 1층', 36.4330183, 127.3855042, '042-300-3099',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '폴드 앤 베이크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '연미당디저트', '유성구', '대전광역시 유성구 용산2로 16-1 1층 101호', 36.4210244, 127.3974546, '010-6451-2671',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '연미당디저트');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)  -- TODO 상세 주소 미확인(동 단위)
SELECT '이프리', '유성구', '대전광역시 유성구 복용동', NULL, NULL, NULL,
       '발사믹&토마토카프리제 샐러드, 치킨텐더 샌드위치',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '이프리');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '다인', '유성구', '대전광역시 유성구 지족로349번길 28 1층', 36.3880430, 127.3160919, '042-331-1041',
       '말차초코우키시마, 말차바스크치즈케이크',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '다인');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '레서', '유성구', '대전광역시 유성구 어은로51번길 10 1층', 36.3627445, 127.3561254, '0502-5551-1267',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '레서');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '오브베이커리', '유성구', '대전광역시 유성구 도안대로 512-21', 36.3459946, 127.3415131, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '오브베이커리');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '밀랑베이크', '유성구', '대전광역시 유성구 테크노3로 80 대전관평예미지어반코어 101동 1층 113호', 36.4288639, 127.3885685, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '밀랑베이크');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '정쿠키', '유성구', '대전광역시 유성구 학하남로19번길 25', 36.3405443, 127.3038170, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '정쿠키');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '스프링벅커피', '유성구', '대전광역시 유성구 테크노4로 98-8 평원오피스텔 1층 101호', 36.4264915, 127.3891822, '042-936-7779',
       '피스타치오 퀸아망, 블루베리 퀸아망',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '스프링벅커피');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '카멜로', '유성구', '대전광역시 유성구 원신흥남로27번길 13', 36.3347079, 127.3372632, '0507-1346-2911',
       '다쿠아즈(밀가루 無), 호두피칸파이',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '카멜로');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '글리하우스', '유성구', '대전광역시 유성구 유성대로 617-14', 36.3497469, 127.3261184, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '글리하우스');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '엠버플로', '유성구', '대전광역시 유성구 원신흥남로42번길 11', 36.3362496, 127.3388343, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '엠버플로');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '르호지에', '유성구', '대전광역시 유성구 엑스포로151번길 19 도룡하우스디 2층 A211호', 36.3761518, 127.3953828, '042-716-7474',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '르호지에');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '코지오이코스', '유성구', '대전광역시 유성구 전민로26번길 31 1층', 36.3981700, 127.4020397, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '코지오이코스');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '피브완', '유성구', '대전광역시 유성구 엑스포로123번길 34 도룡코아루 102동 1층 125호', 36.3763618, 127.3939816, '010-5387-3379',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '피브완');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '일공일일', '유성구', '대전광역시 유성구 신성로 56', 36.3886554, 127.3476789, NULL,
       '청양고추 에그타르트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '일공일일');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '비비스 미트파이 오븐', '유성구', '대전광역시 유성구 어은로51번길 30', 36.3623695, 127.3552149, '042-867-1018',
       '아이리쉬 스튜 비프파이, 스파이시 치킨 굴라쉬 파이',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '비비스 미트파이 오븐');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '로라네방앗간', '유성구', '대전광역시 유성구 반석로11번길 65 1층', 36.3893795, 127.3146706, '010-2711-5106',
       '수제 잠봉 뵈르, 잠봉 루꼴라',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '로라네방앗간');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '폴레폴레', '유성구', '대전광역시 유성구 온천로 63 1-2층', 36.3553628, 127.3449306, NULL,
       '밤소금빵, 고구마소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '폴레폴레');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '리틀딜라잇', '유성구', '대전광역시 유성구 어은로57번길 41 1층 102호', 36.3622993, 127.3542689, '010-2961-2756',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '리틀딜라잇');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '꽃나래허브', '유성구', '대전광역시 유성구 어은로 42', 36.3622357, 127.3576918, '042-864-1153',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '꽃나래허브');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '휘어', '유성구', '대전광역시 유성구 궁동로18번길 71 1층 103호', 36.3628832, 127.3512470, '0503-7151-0079',
       '청양명란마요 소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '휘어');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '포카치아 베르데', '유성구', '대전광역시 유성구 노은로 151', 36.3731670, 127.3173792, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '포카치아 베르데');

-- 중구 (15곳)
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)  -- TODO 상세 주소 미확인(동 단위)
SELECT '촉촉디저트 잼살롱', '중구', '대전광역시 중구 선화동', NULL, NULL, NULL,
       '청양고추토마토잼, 무화과잼',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '촉촉디저트 잼살롱');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '포달러', '중구', '대전광역시 중구 선화서로29번길 24 1-3층', 36.3263784, 127.4175873, '010-5566-5615',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '포달러');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '파이가든', '중구', '대전광역시 중구 중앙로 124 이화빌딩 1층 103호', 36.3275349, 127.4237770, '042-710-8111',
       '크렘쇼쿠니, 나는 말차',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '파이가든');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '푸우딩', '중구', '대전광역시 중구 계룡로830번길 44', 36.3257719, 127.4054316, NULL,
       '커피푸딩, 초코푸딩',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '푸우딩');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '인터뷰베이커리', '중구', '대전광역시 중구 안영로6번길 31', 36.2894435, 127.3789182, NULL,
       '소금빵, 대파에그타르트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '인터뷰베이커리');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '키키오븐', '중구', '대전광역시 중구 중교로 33 1층 103호', 36.3253420, 127.4234176, '010-3460-6737',
       '샌드베이글, 단호박크림치즈치아바타',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '키키오븐');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '모도상점', '중구', '대전광역시 중구 오류로 8-1 1층', 36.3213478, 127.4054793, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '모도상점');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '모쿠 모쿠', '중구', '대전광역시 중구 목척2길 35 1층', 36.3297716, 127.4253699, '042-222-4172',
       '생크림 누텔라 딸기 크레페, 두바이 초코 수건케이크',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '모쿠 모쿠');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '뮤제', '중구', '대전광역시 중구 대흥로121번길 44 1층', 36.3253924, 127.4249416, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '뮤제');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '리리컬', '중구', '대전광역시 중구 대종로 451 3층', 36.3249322, 127.4277220, '010-5701-2514',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '리리컬');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '호두정', '중구', '대전광역시 중구 보문로 59 1층', 36.3129662, 127.4343782, NULL,
       '팥호두과자, 앙버터',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '호두정');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '평생직장커피', '중구', '대전광역시 중구 계룡로874번길 71 1층', 36.3222434, 127.4073985, '042-522-1516',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '평생직장커피');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '윈터커피로스터스', '중구', '대전광역시 중구 대흥로111번길 34', 36.3246349, 127.4240421, NULL,
       '잠봉소금빵, 쪽파크림치즈소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '윈터커피로스터스');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '올드하우스', '중구', '대전광역시 중구 대종로 467 3층', 36.3262510, 127.4269776, '010-6717-8147',
       '크레이프롤',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '올드하우스');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '산도랑', '중구', '대전광역시 중구 대종로452번길 6', 36.3251712, 127.4283580, NULL,
       '후르츠 산도',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '산도랑');

-- 동구 (7곳)
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '모노스톤', '동구', '대전광역시 동구 대전천동로 586 1층', 36.3321336, 127.4273749, '070-7818-6110',
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '모노스톤');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '안가네대추빵', '동구', '대전광역시 동구 동서대로1668번길 28 1층', 36.3471738, 127.4349604, '010-3425-2920',
       '대추빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '안가네대추빵');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '역전빵', '동구', '대전광역시 동구 대전로 823-2', 36.3328888, 127.4316033, '042-867-5597',
       '공갈빵, 바게트',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '역전빵');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '작은숩과자집', '동구', '대전광역시 동구 성동로 17', 36.3377592, 127.4533826, NULL,
       NULL,
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '작은숩과자집');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '디카페', '동구', '대전광역시 동구 계족로140번길 33 펜타뷰아파트 상가동 1층', 36.3271047, 127.4456779, '010-8828-7025',
       '비건 쌀소금빵',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '디카페');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '꿈1993', '동구', '대전광역시 동구 용운로 155 1층', 36.3281921, 127.4596224, NULL,
       '꿈돌이샌드, 꿈순이샌드',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '꿈1993');
INSERT INTO bakery (name, region, address, lat, lng, phone, signature_menu, summary, content)
SELECT '하하메모리즈', '동구', '대전광역시 동구 태전로 4 1층 101호', 36.3311600, 127.4313074, '0507-1331-9852',
       '에그타르트(6개입 선물포장)',
       '2026 대전빵축제 참여 빵집', @fest
 WHERE NOT EXISTS (SELECT 1 FROM bakery WHERE name = '하하메모리즈');
