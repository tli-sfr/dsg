# Directory Sync Service / Gateway — Architectural Design

**Source:** Confluence Arch space, page 914831306  
**Status:** Architectural review request  
**Translated into repo-local markdown.** Unified with PRD: [overview.md](overview.md).

---

## 1. Background

### 1.1 Problem statement

Directory is the source of truth for user identity and can provision users into service providers (SP). RingCentral offers Azure, Okta, Google, OneLogin directory apps for PBX-style sync; only Okta syncs phone number RC → cloud. Apps are 6–7 years old and unsuitable for multi-product RC. **RC will host directory sync** calling cloud directory APIs.

Customers want template id, role id, licenses on new users. RC-managed aspects may change in Service Web and are not directory source of truth — **rules apply these at provisioning only**, not re-enforced on every update.

### 1.2 Requirements (wiki references)

- Essential provisioning: cloud directory → RC
- Custom sync with additional assignment: [GSP-2121](https://jira.ringcentral.com/browse/GSP-2121)
- Sync RC phone info back to cloud directory
- AppSec: [ASWIP-2034](https://jira.ringcentral.com/browse/ASWIP-2034)

### 1.3 Current solution

Azure, Okta, Google, OneLogin for provision/update; only Okta bidirectional for phone number.

---

## 2. Implementation overview

Hosted in **AWS**: Aurora MySQL (**DSB**), **SQS** (**DSQ**), **K8s** service nodes (**DSG**).

| Component | Role |
|-----------|------|
| **DSB** | Sync metadata, history, user hash for change detection |
| **DSQ** | Job queue + job-detail queue |
| **DSG** | APIs for Service Web; publishers scan DSB → DSQ; workers sync users |

**Integration path:** Service Web (SCL) → JWL API → DSG API.

### Metadata configured via DSG

- Account UID, sync type (Full / Incremental / On-Demand)
- Sync direction (Directory → RC, RC → Directory)
- Frequency (incremental)
- Attribute mapping (bidirectional + rule-based)
- Sync rules: AD group, template, role, licenses, site, call queue (TBD)

---

## 3. Major flows

### 3.1 Authentication

- **Target (wiki):** ETM stores subscriberId; credentials in ETM
- **Phase 1 (ETM deferred):** `account_directory_oauth` holds encrypted `client_id` / `client_secret`; [DirectoryAuthPort](../adr/008-directory-auth-port.md) supplies tokens
- DSB `account_directory_auth`: account UID, directory type, group, `etm_subscriber_id` (nullable)

### 3.2 Configuration flow

1. Group assignment → `account_directory_auth.directory_group_id`
2. Rules → `provisioning_assignment_rule`, assignments (license, template, device, area code)
3. Test run: select users from directory group; one-way sync sequence

### 3.3 Job state machine

1. **Job Manager** creates `job` (Pending), enqueues to Job SQS
2. **Job Detail DB publisher (retrieval):** InPrep → batch directory/RC users → `job_detail` (Pending); 429 → re-queue job with backoff
3. **Job Detail DB publisher (publishing):** When complete → job Ready; update `directory_sync_checkpoint`
4. **Job Detail Queue publisher:** Ready job + Pending details → Job Detail SQS; detail → InSync
5. **Worker:** Process user; 429 retry at detail level; Succeeded/Failed; update `directory_sync_user_hash`
6. **Job consolidation:** Completed when all details terminal; Cancelling path; Stuck monitoring

### 3.4 Scheduler

- Scan `directory_sync_time` when `current_time >= next_job_start_time` and last state terminal
- Full sync on admin trigger
- On-demand / failed-user retry: Service Web creates job/job_detail directly (UI TBD in wiki)

### 3.5 Worker

- **Directory → RC:** Rule decides provision; hash compare before sync
- **Directory → RC (updates, Phase 1):** No separate “update rules.” After directory fetch, if `attribute_mapping` or `custom_attribute_mapping` values changed vs `directory_sync_user_hash`, apply UPDATE. Provisioning rules and `rule_based_attribute_mapping` run on **CREATE only**. See [ADR-003](../adr/003-rule-triggers-and-action-sets.md).
- **RC → Directory:** Full sync for linked users; hash optimization
- **429:** Account/batch level on job queue; user level on detail queue
- **Errors:** Log for report; expired token may stop batch; manual retry from report

---

## 4. Database design (DSB)

See [../db/dsb-schema-wiki.md](../db/dsb-schema-wiki.md) (all **29 wiki tables**), [../db/erd.md](../db/erd.md), [../db/schema-auth-extensions.md](../db/schema-auth-extensions.md) (Phase 1 `account_directory_oauth`), and [../db/README.md](../db/README.md).

### 4.1 Authentication

- `directory_type` — Azure, Okta, Google, OneLogin
- `account_directory_auth` — one directory type per account; `etm_subscriber_id`, `directory_group_id`, `active`
- `directory_sync_checkpoint` — `checkpoint_time` (Okta) or `checkpoint_url` (Azure delta)

### 4.2 Settings

**Attribute mapping (create + modify):**

- `sync_direction`, `rc_attribute`, `directory_attribute`
- `attribute_mapping`, `custom_attribute_mapping`
- `rc_rule_based_attribute`, `rule_based_attribute_mapping` (role, site, cost center)

**One-time application (create only):**

- `provisioning_assignment_rule` — priority, `selection_expression`
- `license_assignment`, `template_assignment`, `device_assignment`
- `dl_area_code_assignment`, `dl_area_code_type` — **Phase 1: inventory numbers only** (no purchase); see [ADR-006](../adr/006-number-assignment-inventory-only.md)
- `deprovisioning_rule`, `deprovisioning_type` — FULL_DELETE, RECLAIM_RESOURCE, DISABLE_ONLY

**Sync configuration:**

- `directory_sync_time`, `job_frequency_type`
- `directory_sync_user_hash` — SHA-256 external and RC hashes

### 4.3 Jobs / report

> **P0-9:** Wiki tables support reporting data. Automatic **email** on job completion is **next phase**; Phase 1 adds report query API. See [ADR-005](../adr/005-sync-report-notification.md).

- `job_type` — FULL, INCREMENTAL, ON_DEMAND
- `job_state` — PENDING, IN_PREP, READY, IN_SYNC, COMPLETED, CANCELLED, CANCELLING, SUCCEEDED, FAILED, STUCK
- `operation_type` — CREATE, UPDATE, DELETE
- `job`, `job_detail` — per-user status, `rule_id`, `source_payload`, `comment`

---

## 5. Queue design (DSQ)

| Queue | Purpose |
|-------|---------|
| Job queue | Account-level jobs; batch 429 retry |
| Job detail queue | One user per message; worker consumes individually |

---

## 6. APIs (wiki 2.6)

Documented in [../api/dsg-openapi.yaml](../api/dsg-openapi.yaml):

- `POST/PUT/GET /dsg/v1/{account_id}/directory`
- `POST /dsg/v1/{account_id}/scheduler`
- `POST /dsg/v1/{account_id}/attribute-mapping`
- `POST /dsg/v1/{account_id}/rule`

---

## 7. Compliance and operations

- **EU:** Separate directory sync system for EU data privacy
- **PII/CPNI:** Retention policy for reports and logs; `job_detail.source_payload` may contain PII for **audit + retry** — [ADR-009](../adr/009-job-detail-source-payload-pii.md)
- **HA:** DSB, DSQ, DSG multi-AZ; DSG any AZ covers failover
- **SLA:** Full sync within one day of scheduled start
- **Scale:** K8s autoscaling
- **Monitoring:** Wiki section 3.3 was empty — defined in [overview.md](overview.md#monitoring)
- **Rollout:** Service parameter for beta control
- **Routing:** Account POD → DSG endpoint (not brand-only long term)

### IDP APIs (v1) — via directory SDKs (Gap 5)

Phase 1 uses **vendor SDKs**, not SCIM. See [ADR-004](../adr/004-directory-sdk-adapters.md). Gap 4 in [gap-resolution.md](../prd/gap-resolution.md) is **ElasticMQ/SQS only**.

| IDP | Group lookup | Members | Change detection |
|-----|--------------|---------|------------------|
| Azure | Graph groups filter | members / delta | `@odata.deltaLink` |
| Okta | groups search | group users | System log API |
| Google | Admin SDK groups | members | (per Google API) |

---

## 8. Cost estimate (wiki)

~$16,082 / 12 months (EKS, 4 EC2, 2 Aurora MySQL, SQS) — production reference; local dev uses MySQL + ElasticMQ per team stack.
