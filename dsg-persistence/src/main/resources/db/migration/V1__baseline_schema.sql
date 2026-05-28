-- DSB baseline schema: 29 wiki tables + account_directory_oauth (Phase 1)
-- Source: docs/db/dsb-schema-wiki.md, docs/db/schema-auth-extensions.md

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Lookup / metadata (no FK dependencies)
-- ---------------------------------------------------------------------------

CREATE TABLE directory_type (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    directory_type  VARCHAR(64)  NOT NULL,
    description     VARCHAR(256) NULL,
    UNIQUE KEY uk_directory_type (directory_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sync_direction (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    description  VARCHAR(64) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rc_attribute (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    attribute_name  VARCHAR(64)  NOT NULL,
    attribute_path  VARCHAR(256) NULL,
    display_name    VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE license_type (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(64) NOT NULL,
    description  VARCHAR(64) NULL,
    UNIQUE KEY uk_license_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dl_area_code_type (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    area_code_rule_type VARCHAR(64)  NOT NULL,
    description         VARCHAR(256) NULL,
    UNIQUE KEY uk_area_code_rule_type (area_code_rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE template_type (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(64) NOT NULL,
    description  VARCHAR(64) NULL,
    UNIQUE KEY uk_template_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device_type (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(64) NOT NULL,
    description  VARCHAR(64) NULL,
    UNIQUE KEY uk_device_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deprovisioning_type (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(64)  NOT NULL,
    description  VARCHAR(256) NULL,
    UNIQUE KEY uk_deprovisioning_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE job_type (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    type  VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_job_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE job_state (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    state  VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_job_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operation_type (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    type  VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_operation_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE job_frequency_type (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    frequency_type         VARCHAR(64) NOT NULL,
    frequency_display_name VARCHAR(64) NULL,
    UNIQUE KEY uk_job_frequency_type (frequency_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rc_rule_based_attribute (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    attribute_name  VARCHAR(64)  NOT NULL,
    display_name    VARCHAR(128) NULL,
    UNIQUE KEY uk_rc_rule_based_attribute (attribute_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Directory attributes
-- ---------------------------------------------------------------------------

CREATE TABLE directory_attribute (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    directory_type_id  INT          NOT NULL,
    attribute_name     VARCHAR(64)  NOT NULL,
    attribute_path     VARCHAR(256) NULL,
    description        VARCHAR(128) NULL,
    CONSTRAINT fk_directory_attribute_type
        FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    KEY idx_directory_attribute_type (directory_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Authentication (Phase 1: account_directory_oauth)
-- ---------------------------------------------------------------------------

CREATE TABLE account_directory_oauth (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id                      VARCHAR(64)   NOT NULL,
    directory_type_id               INT           NOT NULL,
    auth_flow                       VARCHAR(32)   NOT NULL,
    client_id                       VARCHAR(512)  NULL,
    client_secret_enc               VARCHAR(2048) NULL,
    azure_tenant_id                 VARCHAR(64)   NULL,
    okta_domain                     VARCHAR(256)  NULL,
    google_workspace_admin          VARCHAR(256)  NULL,
    google_service_account_key_enc  TEXT          NULL,
    refresh_token_enc               VARCHAR(2048) NULL,
    access_token_enc                VARCHAR(2048) NULL,
    access_token_expires_at         TIMESTAMP     NULL,
    scopes                          VARCHAR(1024) NULL,
    created_on                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_directory_type
        FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    UNIQUE KEY uk_oauth_account_directory (account_id, directory_type_id),
    KEY idx_oauth_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE account_directory_auth (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id          VARCHAR(64)  NOT NULL,
    directory_type_id   INT          NOT NULL,
    directory_group_id  VARCHAR(256) NULL,
    etm_subscriber_id   VARCHAR(256) NULL,
    oauth_config_id     BIGINT       NULL,
    active              INT          NOT NULL DEFAULT 0,
    created_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_directory_type
        FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    CONSTRAINT fk_auth_oauth_config
        FOREIGN KEY (oauth_config_id) REFERENCES account_directory_oauth (id),
    UNIQUE KEY uk_auth_account_directory (account_id, directory_type_id),
    KEY idx_auth_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE directory_sync_checkpoint (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id          VARCHAR(64)  NOT NULL,
    directory_type_id   INT          NOT NULL,
    checkpoint_time     TIMESTAMP    NULL,
    checkpoint_url      VARCHAR(4096) NULL,
    created_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_checkpoint_directory_type
        FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    UNIQUE KEY uk_checkpoint_account_directory (account_id, directory_type_id),
    KEY idx_checkpoint_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Mappings
-- ---------------------------------------------------------------------------

CREATE TABLE attribute_mapping (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id                VARCHAR(64)  NOT NULL,
    rc_attribute_id           INT          NOT NULL,
    direction_id              INT          NOT NULL,
    directory_attribute_id    INT          NOT NULL,
    directory_attribute_path  VARCHAR(256) NULL,
    created_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attr_map_rc_attribute
        FOREIGN KEY (rc_attribute_id) REFERENCES rc_attribute (id),
    CONSTRAINT fk_attr_map_direction
        FOREIGN KEY (direction_id) REFERENCES sync_direction (id),
    CONSTRAINT fk_attr_map_directory_attribute
        FOREIGN KEY (directory_attribute_id) REFERENCES directory_attribute (id),
    KEY idx_attr_map_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE custom_attribute_mapping (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id                VARCHAR(64)  NOT NULL,
    directory_attribute_path  VARCHAR(512) NOT NULL,
    directory_attribute_value VARCHAR(256) NULL,
    rc_custom_attribute_name  VARCHAR(256) NULL,
    rc_custom_attribute_id    BIGINT       NULL,
    rc_custom_attribute_value VARCHAR(256) NULL,
    created_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_custom_attr_map_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Provisioning rules and assignments
-- ---------------------------------------------------------------------------

CREATE TABLE provisioning_assignment_rule (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name            VARCHAR(256) NOT NULL,
    account_id           VARCHAR(64)  NOT NULL,
    priority             INT          NOT NULL,
    selection_expression VARCHAR(2048) NULL,
    created_on           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_account_priority (account_id, priority),
    KEY idx_rule_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE license_assignment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    license_type_id INT    NOT NULL,
    license_id      VARCHAR(256) NOT NULL,
    created_on      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_license_assignment_rule
        FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    CONSTRAINT fk_license_assignment_type
        FOREIGN KEY (license_type_id) REFERENCES license_type (id),
    KEY idx_license_assignment_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dl_area_code_assignment (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id                BIGINT NOT NULL,
    area_code_rule_type_id INT    NOT NULL,
    area_code_list         VARCHAR(2048) NULL,
    created_on             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_area_code_assignment_rule
        FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    CONSTRAINT fk_area_code_assignment_type
        FOREIGN KEY (area_code_rule_type_id) REFERENCES dl_area_code_type (id),
    KEY idx_area_code_assignment_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE template_assignment (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id          BIGINT NOT NULL,
    template_type_id INT    NOT NULL,
    template_id      VARCHAR(256) NOT NULL,
    created_on       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_assignment_rule
        FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    CONSTRAINT fk_template_assignment_type
        FOREIGN KEY (template_type_id) REFERENCES template_type (id),
    KEY idx_template_assignment_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device_assignment (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id        BIGINT NOT NULL,
    device_type_id INT    NOT NULL,
    device_id      VARCHAR(256) NULL,
    created_on     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_assignment_rule
        FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    CONSTRAINT fk_device_assignment_type
        FOREIGN KEY (device_type_id) REFERENCES device_type (id),
    KEY idx_device_assignment_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rule_based_attribute_mapping (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id                VARCHAR(64)  NOT NULL,
    rule_id                   BIGINT       NOT NULL,
    directory_attribute_path  VARCHAR(512) NOT NULL,
    directory_attribute_value VARCHAR(256) NULL,
    rc_rule_based_attribute_id INT         NOT NULL,
    rc_object_id              VARCHAR(64)  NULL,
    created_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rule_based_mapping_rule
        FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    CONSTRAINT fk_rule_based_mapping_rc_attr
        FOREIGN KEY (rc_rule_based_attribute_id) REFERENCES rc_rule_based_attribute (id),
    KEY idx_rule_based_mapping_rule (rule_id),
    KEY idx_rule_based_mapping_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deprovisioning_rule (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id             VARCHAR(64) NOT NULL,
    deprovisioning_type_id INT         NOT NULL,
    created_on             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_deprovisioning_rule_type
        FOREIGN KEY (deprovisioning_type_id) REFERENCES deprovisioning_type (id),
    UNIQUE KEY uk_deprovisioning_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Jobs / reporting
-- ---------------------------------------------------------------------------

CREATE TABLE job (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_id             INT          NOT NULL,
    directory_type_id   INT          NOT NULL,
    direction_id        INT          NOT NULL,
    account_id          VARCHAR(64)  NOT NULL,
    rule_id             BIGINT       NULL,
    state_id            INT          NOT NULL,
    created_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_type FOREIGN KEY (type_id) REFERENCES job_type (id),
    CONSTRAINT fk_job_directory_type FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    CONSTRAINT fk_job_direction FOREIGN KEY (direction_id) REFERENCES sync_direction (id),
    CONSTRAINT fk_job_state FOREIGN KEY (state_id) REFERENCES job_state (id),
    CONSTRAINT fk_job_rule FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    KEY idx_job_account (account_id),
    KEY idx_job_account_state (account_id, state_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE job_detail (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id         BIGINT       NOT NULL,
    mailbox_id     VARCHAR(64)  NULL,
    external_id    VARCHAR(256) NULL,
    state_id       INT          NOT NULL,
    operation_id   INT          NOT NULL,
    rule_id        BIGINT       NULL,
    source_payload TEXT         NULL,
    comment        VARCHAR(512) NULL,
    created_on     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_detail_job FOREIGN KEY (job_id) REFERENCES job (id),
    CONSTRAINT fk_job_detail_state FOREIGN KEY (state_id) REFERENCES job_state (id),
    CONSTRAINT fk_job_detail_operation FOREIGN KEY (operation_id) REFERENCES operation_type (id),
    CONSTRAINT fk_job_detail_rule FOREIGN KEY (rule_id) REFERENCES provisioning_assignment_rule (id),
    KEY idx_job_detail_job (job_id),
    KEY idx_job_detail_external (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE directory_sync_time (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id            VARCHAR(64)  NOT NULL,
    directory_type_id     INT          NOT NULL,
    job_type_id           INT          NOT NULL,
    direction_id          INT          NOT NULL,
    latest_job_id         BIGINT       NULL,
    latest_job_start_time TIMESTAMP    NULL,
    latest_job_end_time   TIMESTAMP    NULL,
    latest_job_state      INT          NULL,
    frequency_id          INT          NULL,
    cron_expression       VARCHAR(256) NULL,
    next_job_start_time   TIMESTAMP    NULL,
    created_on            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sync_time_directory_type FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    CONSTRAINT fk_sync_time_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id),
    CONSTRAINT fk_sync_time_direction FOREIGN KEY (direction_id) REFERENCES sync_direction (id),
    CONSTRAINT fk_sync_time_latest_job FOREIGN KEY (latest_job_id) REFERENCES job (id),
    CONSTRAINT fk_sync_time_latest_state FOREIGN KEY (latest_job_state) REFERENCES job_state (id),
    CONSTRAINT fk_sync_time_frequency FOREIGN KEY (frequency_id) REFERENCES job_frequency_type (id),
    UNIQUE KEY uk_sync_time_account_directory (account_id, directory_type_id),
    KEY idx_sync_time_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE directory_sync_user_hash (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id          VARCHAR(64)  NOT NULL,
    directory_type_id   INT          NOT NULL,
    external_id         VARCHAR(256) NOT NULL,
    external_user_hash  VARCHAR(64)  NULL,
    mailbox_id          VARCHAR(64)  NULL,
    rc_user_hash        VARCHAR(64)  NULL,
    created_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_hash_directory_type FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    UNIQUE KEY uk_user_hash_account_external (account_id, directory_type_id, external_id),
    KEY idx_user_hash_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
