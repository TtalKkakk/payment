-- =============================================================
-- V3: 부하테스트용 Mock 가맹점 데이터 삽입
--
-- api_key    : pk_merchant_testmerchant000000000001  (pk_merchant_ 12자 + 24자)
-- api_secret : sk_merchant_testsecret00000000000001  (sk_merchant_ 12자 + 24자)
-- =============================================================
INSERT INTO merchants (id, api_key, api_secret, name, application_id, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'pk_merchant_testmerchant000000000001',
    'sk_merchant_testsecret00000000000001',
    '테스트가맹점',
    NULL,
    'ACTIVE'
);
