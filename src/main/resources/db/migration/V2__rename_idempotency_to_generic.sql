-- =============================================================
-- V2: idempotency 테이블·컬럼을 도메인 독립적인 이름으로 변경
--   payment_idempotency_keys → idempotency_keys
--   payment_id              → resource_id
--   uk_payment_idem_merchant_key → uk_idem_merchant_key
-- =============================================================

RENAME TABLE payment_idempotency_keys TO idempotency_keys;

ALTER TABLE idempotency_keys
    RENAME COLUMN payment_id TO resource_id,
    DROP INDEX uk_payment_idem_merchant_key,
    ADD CONSTRAINT uk_idem_merchant_key UNIQUE (merchant_id, idempotency_key);
