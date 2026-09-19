-- =============================================================
-- V18 되돌리기: 주소를 가이드북 표기(오타 포함) 그대로 되돌린다.
-- 좌표는 건드리지 않았으므로 복구할 것이 없다.
-- =============================================================
UPDATE bakery SET address = '대전광역시 동구 화남로 275번길 123'
 WHERE name = '롤라' AND address = '대전광역시 동구 회남로 275번길 123';

UPDATE bakery SET address = '대전광역시 중구 계룡로 79-24, 1층'
 WHERE name = '굿베이글' AND address = '대전광역시 중구 과례로 79-24, 1층';

UPDATE bakery SET address = '대전광역시 서구 원도안길 241번길 24-46, 1층'
 WHERE name = '소솜' AND address = '대전광역시 서구 원도안로 241번길 24-46, 1층';

UPDATE bakery SET address = '대전광역시 서구 낭선로 9번길 47-3, 1층'
 WHERE name = '시오네베이크샵' AND address = '대전광역시 서구 남선로 9번길 47-3, 1층';

UPDATE bakery SET address = '대전광역시 서구 월평복로 11, 주공아파트1단지상가 104호'
 WHERE name = '당신을 위한 빵집' AND address = '대전광역시 서구 월평북로 11, 주공아파트1단지상가 104호';
