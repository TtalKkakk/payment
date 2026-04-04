-- =============================================================
-- V1: 초기 스키마 (Flyway 도입 시점의 전체 테이블 정의)
-- 이미 DB가 존재하는 경우 baseline-on-migrate: true 로 이 버전은 건너뜀
-- =============================================================

CREATE TABLE IF NOT EXISTS merchants (
    id             VARCHAR(36)  NOT NULL,
    api_key        VARCHAR(100) NOT NULL,
    api_secret     VARCHAR(100) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    application_id VARCHAR(36),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchants_api_key (api_key),
    UNIQUE KEY uk_merchants_application_id (application_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS merchant_applications (
    id              VARCHAR(36)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    business_number VARCHAR(20)  NOT NULL,
    phone           VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    reject_reason   VARCHAR(500),
    password_hash   VARCHAR(60)  NOT NULL,
    created_at      DATETIME(6),
    processed_at    DATETIME(6),
    updated_at      DATETIME(6),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_applications_business_number (business_number)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS card_companies (
    id            VARCHAR(36)  NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    base_url      VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_card_companies_code (code)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id                    VARCHAR(36)   NOT NULL,
    merchant_id           VARCHAR(36)   NOT NULL,
    amount                BIGINT        NOT NULL,
    status                VARCHAR(30)   NOT NULL,
    merchant_order_id     VARCHAR(100)  NOT NULL,
    order_name            VARCHAR(200)  NOT NULL,
    customer_email        VARCHAR(200),
    customer_name         VARCHAR(100)  NOT NULL,
    callback_url          VARCHAR(500)  NOT NULL,
    approval_number       VARCHAR(50),
    transaction_id        VARCHAR(100),
    approved_at           DATETIME(6),
    last_failure_category VARCHAR(20),
    last_failure_code     VARCHAR(100),
    last_failure_message  VARCHAR(2000),
    last_failure_at       DATETIME(6),
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    card_company_id       VARCHAR(36)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_merchant_id_merchant_order_id (merchant_id, merchant_order_id),
    CONSTRAINT fk_payments_card_company FOREIGN KEY (card_company_id) REFERENCES card_companies (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS receipts (
    id                VARCHAR(36)  NOT NULL,
    receipt_number    VARCHAR(50)  NOT NULL,
    payment_id        VARCHAR(36)  NOT NULL,
    merchant_id       VARCHAR(36)  NOT NULL,
    merchant_name     VARCHAR(200) NOT NULL,
    amount            BIGINT       NOT NULL,
    order_name        VARCHAR(200),
    merchant_order_id VARCHAR(100),
    customer_name     VARCHAR(100),
    approval_number   VARCHAR(50),
    transaction_id    VARCHAR(100),
    approved_at       DATETIME(6),
    status            VARCHAR(20)  NOT NULL,
    created_at        DATETIME(6),
    updated_at        DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_receipts_receipt_number (receipt_number),
    UNIQUE KEY uk_receipts_payment_id (payment_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS retry_jobs (
    id                 VARCHAR(36)  NOT NULL,
    job_type           VARCHAR(100) NOT NULL,
    idempotency_key    VARCHAR(200) NOT NULL,
    payload_json       LONGTEXT     NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    attempt_count      INT          NOT NULL,
    max_attempts       INT          NOT NULL,
    next_run_at        DATETIME(6)  NOT NULL,
    expires_at         DATETIME(6)  NOT NULL,
    last_error_message VARCHAR(2000),
    last_failed_at     DATETIME(6),
    locked_until       DATETIME(6),
    locked_by          VARCHAR(100),
    lock_token         VARCHAR(36),
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_retry_jobs_type_key (job_type, idempotency_key),
    INDEX ix_retry_jobs_due (status, next_run_at, expires_at),
    INDEX ix_retry_jobs_lock (status, locked_until)
) ENGINE = InnoDB;

-- 멱등성 키 테이블 (payment 전용 원본 이름 — V2에서 범용 이름으로 변경)
CREATE TABLE IF NOT EXISTS payment_idempotency_keys (
    id              VARCHAR(36)  NOT NULL,
    merchant_id     VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    payment_id      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_idem_merchant_key (merchant_id, idempotency_key)
) ENGINE = InnoDB;
