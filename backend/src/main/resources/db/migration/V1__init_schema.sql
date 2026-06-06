-- LabAssist initial schema
-- UUID primary keys are supplied by the application (Hibernate GenerationType.UUID).
-- Timestamps are timestamptz to align with java.time.Instant.

create table app_user (
    id            uuid primary key,
    username      varchar(64)  not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(128),
    role          varchar(16)  not null,
    enabled       boolean      not null default true,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null
);

create table reference_range (
    id            uuid primary key,
    code          varchar(32) not null,
    name          varchar(128),
    unit          varchar(32),
    sex           varchar(16),
    age_min       integer,
    age_max       integer,
    low           numeric(14, 3),
    high          numeric(14, 3),
    critical_low  numeric(14, 3),
    critical_high numeric(14, 3)
);
create index idx_reference_range_code on reference_range (code);

create table lab_report (
    id                  uuid primary key,
    external_id         varchar(64) not null unique,
    device_id           varchar(64),
    patient_name        varchar(512),   -- AES-256-GCM ciphertext (Base64)
    patient_mrn         varchar(512),   -- AES-256-GCM ciphertext (Base64)
    patient_age         integer,
    patient_sex         varchar(16),
    sample_collected_at timestamptz,
    received_at         timestamptz not null,
    status              varchar(16) not null,
    overall_abnormal    boolean     not null default false,
    abnormal_count      integer     not null default 0,
    critical_count      integer     not null default 0,
    raw_payload         jsonb,
    rejection_reason    varchar(512),
    created_at          timestamptz not null,
    updated_at          timestamptz not null
);
create index idx_lab_report_received_at on lab_report (received_at);
create index idx_lab_report_status on lab_report (status);
create index idx_lab_report_overall_abnormal on lab_report (overall_abnormal);

create table test_result (
    id            uuid primary key,
    lab_report_id uuid        not null references lab_report (id) on delete cascade,
    code          varchar(32) not null,
    name          varchar(128),
    value         numeric(14, 3),
    value_text    varchar(128),
    unit          varchar(32),
    ref_low       numeric(14, 3),
    ref_high      numeric(14, 3),
    flag          varchar(16) not null,
    created_at    timestamptz not null,
    updated_at    timestamptz not null
);
create index idx_test_result_report on test_result (lab_report_id);

create table llm_interpretation (
    id             uuid primary key,
    lab_report_id  uuid        not null references lab_report (id) on delete cascade,
    model          varchar(64),
    prompt_version varchar(32),
    response_text  text,
    status         varchar(16) not null,
    latency_ms     bigint,
    created_by     varchar(64),
    error_message  varchar(1024),
    created_at     timestamptz not null,
    updated_at     timestamptz not null
);
create index idx_llm_interpretation_report on llm_interpretation (lab_report_id);

create table audit_log (
    id          uuid primary key,
    at          timestamptz not null,
    username    varchar(64),
    action      varchar(32) not null,
    outcome     varchar(16) not null,
    entity_type varchar(64),
    entity_id   varchar(64),
    details     jsonb,
    ip_address  varchar(64)
);
create index idx_audit_log_at on audit_log (at);
create index idx_audit_log_action on audit_log (action);
