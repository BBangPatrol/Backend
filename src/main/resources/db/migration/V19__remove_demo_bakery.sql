-- =============================================================
-- V19: 시연용 빵집('꿈돌제과 시연점') 제거
--
-- V9 가 넣은 가상의 가게와 그에 딸린 방문·리뷰·좋아요를 지운다.
-- V9 는 빈 DB 에서 다시 실행되므로, 운영 DB 에서 손으로 지워도 새로 구축하면 되살아난다.
-- 그 경로를 막기 위해 삭제도 마이그레이션으로 둔다.
--
-- FK 에 ON DELETE CASCADE 가 없어 자식부터 순서대로 지운다.
-- id 가 아니라 시연용 사업자번호(000-00-00000)로 찾으므로, 이미 지워진 DB 에서는
-- @b 가 NULL 이라 모든 DELETE 가 0 건으로 통과한다.
--
-- R2 의 demo/ 프리픽스 오브젝트 12 개는 별도로 지울 것 (SQL 로는 닿지 않는다).
--   demo/bakery-signature.jpg, demo/bakery-signature_thumb.jpg
--   demo/review-1~5.jpg 와 각 _thumb.jpg
-- =============================================================

SET @b = (SELECT id FROM bakery WHERE business_number = '000-00-00000');

DELETE rl FROM review_like rl JOIN review r ON r.id = rl.review_id WHERE r.bakery_id = @b;
DELETE rk FROM review_keyword rk JOIN review r ON r.id = rk.review_id WHERE r.bakery_id = @b;
DELETE ri FROM review_image ri JOIN review r ON r.id = ri.review_id WHERE r.bakery_id = @b;
DELETE FROM review WHERE bakery_id = @b;
DELETE vd FROM visit_detail vd JOIN visits v ON v.id = vd.visit_id WHERE v.bakery_id = @b;
DELETE FROM visits WHERE bakery_id = @b;
DELETE FROM sig_image WHERE bakery_id = @b;
DELETE FROM bookmark WHERE bakery_id = @b;
DELETE FROM bakery WHERE id = @b;
