-- EnterpriseFlow AI Demo: initial schema.
-- IDs are time-ordered UUIDs (v7) stored as BINARY(16); timestamps are DATETIME(6) in UTC.

CREATE TABLE app_user (
    id            BINARY(16)   NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('REVIEWER', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document (
    id                 BINARY(16)   NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    file_size          BIGINT       NOT NULL,
    sha256             CHAR(64)     NOT NULL,
    storage_key        VARCHAR(100) NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    failure_reason     VARCHAR(500) NULL,
    uploaded_at        DATETIME(6)  NOT NULL,
    uploaded_by        BINARY(16)   NOT NULL,
    CONSTRAINT pk_document PRIMARY KEY (id),
    CONSTRAINT uk_document_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_document_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES app_user (id),
    CONSTRAINT ck_document_file_size CHECK (file_size > 0),
    CONSTRAINT ck_document_status CHECK (status IN
        ('UPLOADED', 'PROCESSING', 'EXTRACTED', 'EXTRACTION_FAILED', 'IN_REVIEW', 'CONFIRMED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_document_uploaded_at ON document (uploaded_at DESC);
CREATE INDEX idx_document_status ON document (status);
CREATE INDEX idx_document_sha256 ON document (sha256);

CREATE TABLE order_extraction (
    id                      BINARY(16)    NOT NULL,
    document_id             BINARY(16)    NOT NULL,
    po_number               VARCHAR(100)  NULL,
    po_date                 DATE          NULL,
    customer_name           VARCHAR(200)  NULL,
    customer_email          VARCHAR(254)  NULL,
    customer_phone          VARCHAR(50)   NULL,
    delivery_address        VARCHAR(500)  NULL,
    requested_delivery_date DATE          NULL,
    currency                CHAR(3)       NULL,
    subtotal                DECIMAL(15,2) NULL,
    tax_amount              DECIMAL(15,2) NULL,
    total_amount            DECIMAL(15,2) NULL,
    notes                   VARCHAR(2000) NULL,
    ai_confidence           DECIMAL(3,2)  NULL,
    ai_provider             VARCHAR(50)   NOT NULL,
    ai_model                VARCHAR(100)  NOT NULL,
    ai_raw_result           JSON          NOT NULL,
    extracted_at            DATETIME(6)   NOT NULL,
    reviewed_by             BINARY(16)    NULL,
    reviewed_at             DATETIME(6)   NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_order_extraction PRIMARY KEY (id),
    CONSTRAINT uk_order_extraction_document UNIQUE (document_id),
    CONSTRAINT fk_order_extraction_document FOREIGN KEY (document_id) REFERENCES document (id),
    CONSTRAINT fk_order_extraction_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES app_user (id),
    CONSTRAINT ck_order_extraction_confidence CHECK (ai_confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_order_extraction_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_order_extraction_tax CHECK (tax_amount >= 0),
    CONSTRAINT ck_order_extraction_total CHECK (total_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_extraction_line (
    id              BINARY(16)    NOT NULL,
    extraction_id   BINARY(16)    NOT NULL,
    line_number     INT           NOT NULL,
    product_code    VARCHAR(100)  NULL,
    description     VARCHAR(500)  NULL,
    quantity        DECIMAL(12,3) NULL,
    unit_of_measure VARCHAR(20)   NULL,
    unit_price      DECIMAL(15,2) NULL,
    line_total      DECIMAL(15,2) NULL,
    CONSTRAINT pk_order_extraction_line PRIMARY KEY (id),
    CONSTRAINT uk_order_extraction_line_number UNIQUE (extraction_id, line_number),
    CONSTRAINT fk_order_extraction_line_extraction FOREIGN KEY (extraction_id)
        REFERENCES order_extraction (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_extraction_line_number CHECK (line_number > 0),
    CONSTRAINT ck_order_extraction_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_extraction_line_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_extraction_line_total CHECK (line_total >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_event (
    id          BINARY(16)   NOT NULL,
    document_id BINARY(16)   NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    actor       VARCHAR(100) NOT NULL,
    details     JSON         NULL,
    occurred_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_audit_event PRIMARY KEY (id),
    CONSTRAINT fk_audit_event_document FOREIGN KEY (document_id) REFERENCES document (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_event_document_occurred ON audit_event (document_id, occurred_at);
