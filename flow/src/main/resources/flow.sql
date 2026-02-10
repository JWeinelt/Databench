CREATE SCHEMA IF NOT EXISTS databench_flow;
USE databench_flow;

CREATE TABLE users (
    user_id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username           VARCHAR(100) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    is_admin            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disabled_at        TIMESTAMP NULL
);

CREATE TABLE jobs (
    job_id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name           VARCHAR(200) NOT NULL,
    description        TEXT,
    owner_user_id      BIGINT NOT NULL,
    is_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES users(user_id)
);

CREATE TABLE job_steps (
    step_id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id                  BIGINT NOT NULL,
    step_order              INT NOT NULL,
    step_name               VARCHAR(200) NOT NULL,

    script_path             VARCHAR(500) NOT NULL,

    on_success_action       ENUM('CONTINUE','STOP','JUMP') NOT NULL,
    on_success_jump_step    INT NULL,

    on_error_action         ENUM('CONTINUE','STOP','JUMP','RETRY') NOT NULL,
    on_error_jump_step      INT NULL,

    retry_count             INT NOT NULL DEFAULT 0,
    retry_delay_seconds     INT NOT NULL DEFAULT 0,

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (job_id) REFERENCES jobs(job_id),
    UNIQUE (job_id, step_order)
);

CREATE TABLE data_sources (
    data_source_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    name               VARCHAR(200) NOT NULL,
    db_type            VARCHAR(100) NOT NULL,
    connection_ref     VARCHAR(500) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_data_sources (
    job_id             BIGINT NOT NULL,
    data_source_id     BIGINT NOT NULL,
    PRIMARY KEY (job_id, data_source_id),
    FOREIGN KEY (job_id) REFERENCES jobs(job_id),
    FOREIGN KEY (data_source_id) REFERENCES data_sources(data_source_id)
);

CREATE TABLE job_schedules (
    schedule_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id             BIGINT NOT NULL,
    cron_expression    VARCHAR(100) NOT NULL,
    is_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id)
);

CREATE TABLE job_step_objects (
    step_id            BIGINT NOT NULL,
    object_name        VARCHAR(300) NOT NULL,
    object_type        ENUM('TABLE','VIEW') NOT NULL,
    access_type        ENUM('SELECT','INSERT','UPDATE','DELETE','MERGE') NOT NULL,
    PRIMARY KEY (step_id, object_name, access_type),
    FOREIGN KEY (step_id) REFERENCES job_steps(step_id)
);

CREATE TABLE object_locks (
    object_name        VARCHAR(300) NOT NULL,
    job_run_id         BIGINT NOT NULL,
    locked_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (object_name),
    UNIQUE (object_name)
);

CREATE TABLE job_runs (
    job_run_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id             BIGINT NOT NULL,
    triggered_by       ENUM('SCHEDULE','MANUAL') NOT NULL,
    started_at         TIMESTAMP NOT NULL,
    finished_at        TIMESTAMP NULL,
    status             ENUM('RUNNING','SUCCESS','FAILED','SKIPPED','COLLISION') NOT NULL,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id)
);

CREATE TABLE step_runs (
    step_run_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_run_id         BIGINT NOT NULL,
    step_id            BIGINT NOT NULL,
    started_at         TIMESTAMP NOT NULL,
    finished_at        TIMESTAMP NULL,
    status             ENUM('SUCCESS','FAILED','SKIPPED','RETRY') NOT NULL,
    error_message      TEXT NULL,
    FOREIGN KEY (job_run_id) REFERENCES job_runs(job_run_id),
    FOREIGN KEY (step_id) REFERENCES job_steps(step_id)
);
