# DSB schema — wiki baseline (section 2.4)

Translated from [Directory Sync Service/Gateway Design](../architecture/dsg-design-wiki.md).  
**Extensions (not in wiki):** [schema-auth-extensions.md](schema-auth-extensions.md) (`account_directory_oauth`).

---

## 4.4.1 Directory Sync authentication

### `directory_type`

Cloud directory metadata. Wiki: `client_id` / `client_secret` live in ETM; Phase 1 may use `account_directory_oauth` instead.

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `directory_type` | varchar(64) | Azure, Okta, Google, OneLogin |
| `description` | varchar(256) | |

### `account_directory_auth`

One directory type per account.

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) | RC account |
| `directory_type_id` | int FK | → `directory_type` |
| `directory_group_id` | varchar(256) | AD group for provisioning |
| `etm_subscriber_id` | varchar(256) | ETM connection (nullable Phase 1) |
| `oauth_config_id` | bigint FK | → `account_directory_oauth` (Phase 1 extension) |
| `active` | int | 0 = skip incremental scan |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `directory_sync_checkpoint`

Incremental sync cursor per account / directory type.

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) FK | |
| `directory_type_id` | int FK | |
| `checkpoint_time` | timestamp | Okta-style |
| `checkpoint_url` | varchar(4096) | Azure delta link |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

---

## 4.4.2 Directory Sync settings

### `sync_direction`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | 1 = DIR→RC, 2 = RC→DIR |
| `description` | varchar(64) | |

### `rc_attribute`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `attribute_name` | varchar(64) | |
| `attribute_path` | varchar(256) | Path in RC user extension object |
| `display_name` | varchar(128) | |

### `directory_attribute`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `directory_type_id` | int FK | |
| `attribute_name` | varchar(64) | |
| `attribute_path` | varchar(256) | |
| `description` | varchar(128) | |

### `attribute_mapping`

Account-level mapping (create + modify).

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) FK | |
| `rc_attribute_id` | int FK | |
| `direction_id` | int FK | → `sync_direction` |
| `directory_attribute_id` | int FK | |
| `directory_attribute_path` | varchar(256) | Custom path override |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `custom_attribute_mapping`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) FK | |
| `directory_attribute_path` | varchar(512) | |
| `directory_attribute_value` | varchar(256) | Reserved |
| `rc_custom_attribute_name` | varchar(256) | |
| `rc_custom_attribute_id` | bigint | |
| `rc_custom_attribute_value` | varchar(256) | Reserved |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `rc_rule_based_attribute`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `attribute_name` | varchar(64) | ROLE, SITE, COST_CENTER, USER_GROUP |
| `display_name` | varchar(128) | |

### `rule_based_attribute_mapping`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) FK | |
| `rule_id` | bigint FK | → `provisioning_assignment_rule` |
| `directory_attribute_path` | varchar(512) | |
| `directory_attribute_value` | varchar(256) | |
| `rc_rule_based_attribute_id` | int FK | |
| `rc_object_id` | varchar(64) | Target RC object id |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

---

## One-time application (create / Type 1 provision)

### `provisioning_assignment_rule`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `rule_name` | varchar(256) | |
| `account_id` | varchar(64) | Unique with `priority` |
| `priority` | int | |
| `selection_expression` | varchar(2048) | JSON criteria (optional) |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `license_assignment`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `rule_id` | bigint FK | |
| `license_type_id` | int FK | |
| `license_id` | varchar(256) | RC license id |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `license_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | PRIMARY_LICENSE, ADD_ON_LICENSE, … |
| `description` | varchar(64) | |

### `dl_area_code_assignment`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `rule_id` | bigint FK | |
| `area_code_rule_type_id` | int FK | |
| `area_code_list` | varchar(2048) | JSON area code list |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `dl_area_code_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `area_code_rule_type` | varchar(64) | ADDRESS, SPECIFIED_AREA_CODE |
| `description` | varchar(256) | |

### `template_assignment`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `rule_id` | bigint FK | |
| `template_type_id` | int FK | |
| `template_id` | varchar(256) | |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `template_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | USER, CALL_HANDLING |
| `description` | varchar(64) | |

### `device_assignment`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `rule_id` | bigint FK | |
| `device_type_id` | int FK | |
| `device_id` | varchar(256) | Inventory phone id |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `device_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | BYOD, RingCentral App, Inventory phone |
| `description` | varchar(64) | |

### `deprovisioning_rule`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) FK | |
| `deprovisioning_type_id` | int FK | |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `deprovisioning_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | FULL_DELETE, DISABLE_ONLY, RECLAIM_RESOURCE |
| `description` | varchar(256) | Maps to PRD options A / C / B |

---

## Sync configuration

### `directory_sync_time`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) | |
| `directory_type_id` | int FK | |
| `job_type_id` | int FK | |
| `direction_id` | int FK | |
| `latest_job_id` | bigint FK | |
| `latest_job_start_time` | timestamp | |
| `latest_job_end_time` | timestamp | |
| `latest_job_state` | int FK | → `job_state` |
| `frequency_id` | int FK | |
| `cron_expression` | varchar(256) | |
| `next_job_start_time` | timestamp | Incremental scheduling |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `job_frequency_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `frequency_type` | varchar(64) | DAILY, WEEKLY, MONTHLY |
| `frequency_display_name` | varchar(64) | |

### `directory_sync_user_hash`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `account_id` | varchar(64) | |
| `directory_type_id` | int FK | |
| `external_id` | varchar(256) | Directory user id / email |
| `external_user_hash` | varchar(64) | SHA-256 directory-side |
| `mailbox_id` | varchar(64) | RC extension id after link |
| `rc_user_hash` | varchar(64) | SHA-256 RC-side |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

---

## Jobs / report (section 2.4.2)

### `job_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | FULL, INCREMENTAL, ON_DEMAND |

### `job_state`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `state` | varchar(64) | PENDING, IN_PREP, READY, IN_SYNC, COMPLETED, CANCELLED, CANCELLING, SUCCEEDED, FAILED, STUCK |

### `operation_type`

| Column | Type | Comment |
|--------|------|---------|
| `id` | int PK | |
| `type` | varchar(64) | CREATE, UPDATE, DELETE |

### `job`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `type_id` | int FK | |
| `directory_type_id` | int FK | |
| `direction_id` | int FK | |
| `account_id` | varchar(64) FK | |
| `rule_id` | bigint FK | On-demand only |
| `state_id` | int FK | |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

### `job_detail`

| Column | Type | Comment |
|--------|------|---------|
| `id` | bigint PK | |
| `job_id` | bigint FK | |
| `mailbox_id` | varchar(64) | RC extension after create |
| `external_id` | varchar(256) | Directory id / email |
| `state_id` | int FK | |
| `operation_id` | int FK | |
| `rule_id` | bigint FK | Provision rule used |
| `source_payload` | varchar(2048) | PII OK — audit/retry [ADR-009](../adr/009-job-detail-source-payload-pii.md); consider TEXT if larger |
| `comment` | varchar(512) | Failure reason |
| `created_on` | timestamp | |
| `updated_on` | timestamp | |

---

## Phase 1 extension (not in wiki)

See [schema-auth-extensions.md](schema-auth-extensions.md) — `account_directory_oauth`.

---

## Table count summary

| Wiki section | Tables |
|--------------|--------|
| Authentication | 3 |
| Mapping / metadata | 6 |
| Rules / assignments | 12 |
| Sync config | 3 |
| Jobs / report | 5 |
| **Wiki total** | **29** |
| Phase 1 extension | **+1** (`account_directory_oauth`) |
| **Local DSB total** | **30** |
