# Gap resolution — PRD vs wiki vs local design

Decisions that close validation gaps between the [product PRD](directory-integration-2.0.md) and [architectural wiki](../architecture/dsg-design-wiki.md). Each item links to an ADR where applicable.

---

## Gap 1: Three trigger types (P0-8, P0-6) — **Closed (Phase 1)**

### Original gap

PRD P0-8 requires a unified Rules Table with three trigger types (provision, change, delete). The wiki defines `provisioning_assignment_rule` with priority but does not model Type 2 (user data change) as a separate rule type.

### Decision (agreed, Phase 1)

| Trigger | Phase 1 behavior | Configuration surface |
|---------|------------------|------------------------|
| **Type 1 — Provision (CREATE)** | Explicit rules: `provisioning_assignment_rule` + assignments (license, template, device, area code) + `rule_based_attribute_mapping` | Rules Table (provision + deprovision) |
| **Type 2 — Change (UPDATE)** | **Implicit** — no update rules in Phase 1 | `attribute_mapping` + `custom_attribute_mapping` only |
| **Type 3 — Delete (DELETE)** | Explicit `deprovisioning_rule` (FULL_DELETE, RECLAIM_RESOURCE, DISABLE_ONLY) | Deprovision settings |

**Type 2 detail:** After directory users are retrieved (incremental or full sync), the worker compares mapped fields against `directory_sync_user_hash`. If values for **basic** (`attribute_mapping`) or **custom** (`custom_attribute_mapping`) mappings changed, apply UPDATE to RC. No `selection_expression` re-evaluation on update.

**Explicitly out of scope for Phase 1 update path:**

- `rule_based_attribute_mapping` (role, site, cost center) — apply on **CREATE only**, not on change
- License, template, device assignments — **CREATE only** (aligned with wiki one-time provisioning doctrine)
- Conditional reprovisioning when IDP attributes change (e.g. department transfer → new role) — **deferred** to a later phase

**Reporting:** Implicit updates produce normal `job_detail` rows (`operation` = UPDATE, success/failure, `comment`).

**PRD deviation:** P0-8 UI shows two rule types (Provision, Deprovision) plus a **Mapping** section for ongoing sync—not a third “Update rule” type. PM sign-off required for deferring conditional update rules.

**ADR:** [003-rule-triggers-and-action-sets.md](../adr/003-rule-triggers-and-action-sets.md)

---

## Gap 2: Sync report notification (P0-9) — **Closed (Phase 1 partial; email in next phase)**

### Original gap

PRD P0-9 requires automatic execution result reports (success count, failed users with reasons) delivered via **email** after each job. The wiki persists outcomes in `job` / `job_detail` but does not define notification delivery.

### Decision (agreed)

| Capability | Phase 1 (current) | Next phase |
|------------|-------------------|------------|
| Persist job outcomes | Yes — `job`, `job_detail`, `comment` on failure | Same |
| Success / failure aggregation | Yes — job consolidation + query API | Same |
| Admin views report in Service Web | Yes — via `GET .../jobs/{jobId}/report` (or equivalent) | Enhanced UI (P2-3 dashboard) |
| **Email notification on job complete** | **No — deferred** | **Yes** — `ReportNotifier` worker + templates |

**Phase 1 satisfies** the data and investigability intent of P0-9 (admin can see who failed and why without re-running sync). **Email delivery** is explicitly **next phase** to close the full PRD acceptance criterion.

**ADR:** [005-sync-report-notification.md](../adr/005-sync-report-notification.md)

---

## Gap 3: Number assignment (P0-2) — **Closed (Phase 1 — inventory only)**

### Original gap

PRD P0-2 requires IDP-driven area code logic, choice of number source (inventory vs purchase), PoA handling for on-demand countries, and explicit errors on insufficient inventory. The wiki defines `dl_area_code_assignment` but not purchase or PoA flows.

### Decision (agreed, Phase 1)

| PRD capability | Phase 1 |
|----------------|---------|
| Area code from IDP / rule config | Yes — `dl_area_code_assignment` + `dl_area_code_type` (ADDRESS, SPECIFIED_AREA_CODE) |
| Assign from number inventory / company number | **Yes — only supported source** |
| Purchase new number | **Out of scope** |
| On-demand country / PoA auto-purchase skip | **N/A** (no purchase) |
| Insufficient inventory | Fail `job_detail` with clear `comment`; identify user |

**Scope:** Number assignment runs on **provision (Type 1) only**, consistent with [ADR-003](../adr/003-rule-triggers-and-action-sets.md).

**PRD deviation:** “Purchase new number” and PoA flows deferred to a later phase. PM sign-off required.

**ADR:** [006-number-assignment-inventory-only.md](../adr/006-number-assignment-inventory-only.md)

---

## Gap 4: Queue technology (ElasticMQ vs SQS) — **Closed**

**Topic:** DSQ messaging only (not directory APIs).

ElasticMQ and AWS SQS share an **SQS-compatible API**. The same `MessageQueuePort` and message contracts work in both environments.

| Environment | Queue | Preference |
|-------------|--------|------------|
| **Local / CI** | **ElasticMQ** | **Default for development** — run via [docker-compose.yml](../../docker-compose.yml) |
| Production | AWS SQS | Same adapter interface; real AWS endpoint + IAM |

**Team preference:** Use **ElasticMQ first** locally; switch to SQS only for deployed environments (no code changes in workers beyond configuration/profile).

**ADR:** [002-queue-abstraction.md](../adr/002-queue-abstraction.md)

---

## Gap 5: Directory integration — SDK, not SCIM (P2-1) — **Closed (Phase 1)**

> If you are looking for **cloud directory / IDP** integration, this is **Gap 5**. **Gap 4** is queues only (ElasticMQ/SQS).

### Original gap

PRD P2-1 targets a generic **SCIM** consumer. The wiki uses **per-vendor directory APIs** (Azure Graph, Okta, Google Admin). Unclear whether DSG should build SCIM or follow the wiki.

### Decision (agreed, Phase 1)

| Approach | Phase 1 |
|----------|---------|
| Microsoft Graph / Okta / Google / OneLogin | **Official directory SDKs** per `directory_type` |
| SCIM 2.0 protocol | **Not used** in Phase 1 |
| Integration pattern | `DirectoryPort` interface + one adapter per vendor |
| Auth | ETM (`etm_subscriber_id`); secrets not in DSB |
| P2-1 generic SCIM IDP | **Deferred** — optional `ScimDirectoryAdapter` later on same port |

**ADR:** [004-directory-sdk-adapters.md](../adr/004-directory-sdk-adapters.md)

---

## PLA / RingCentral API — **Closed (Phase 1 partial)**

### Decision

| Category | Phase 1 |
|----------|---------|
| User **create / read / update / delete** | **RingCentral Extensions API** ([createExtension](https://developers.ringcentral.com/api-reference/Extensions/createExtension), update, delete, get, list) |
| Site, role, cost center, phone, templates, devices, multi-license | **`RcProvisioningPort` stubs** until PLA publishes APIs |

**Spec:** [rc-pla-api-spec.md](../api/rc-pla-api-spec.md)  
**ADR:** [007-rc-pla-provisioning-port.md](../adr/007-rc-pla-provisioning-port.md)

Replace stub methods with live PLA/RC clients incrementally without changing worker orchestration.

---

## Gap 6: Directory authentication (ETM deferred) — **Closed (Phase 1)**

### Original gap

Wiki assumes **ETM** holds directory OAuth credentials and exposes `etm_subscriber_id` on `account_directory_auth`. ETM is **not ready** as a dependency; DSG still needs access/refresh tokens for Azure, Okta, and Google directory SDKs.

### Decision (agreed, Phase 1)

| Concern | Phase 1 |
|---------|---------|
| Token access | `DirectoryAuthPort.getAccessToken(accountId, directoryTypeId)` |
| Credential storage | **`account_directory_oauth`** — `client_id`, encrypted `client_secret`, provider fields |
| Account link | `account_directory_auth` (+ optional `oauth_config_id`); `etm_subscriber_id` **nullable** |
| Providers | Azure, Okta, Google token strategies (refresh / client credentials / service account) |
| Admin config | `PUT/GET /dsg/v1/{accountId}/directory/oauth` |
| Future ETM | Profile `dsg.auth=etm` — `EtmDirectoryAuthClient`; migrate secrets out of DSB |

**Security:** `client_id` plain in DSB; `client_secret` (and tokens) **AES-256** encrypted with `dsg.crypto.secret-key` (placeholder in properties locally, **production key at deploy**). Never returned on GET. **AppSec approved** AES-256 + deploy-time key (ASWIP-2034 / ADR-008).

**ADR:** [008-directory-auth-port.md](../adr/008-directory-auth-port.md)  
**Schema:** [schema-auth-extensions.md](../db/schema-auth-extensions.md)  
**API:** [directory-auth-api-spec.md](../api/directory-auth-api-spec.md)

---

## Data handling: `job_detail.source_payload` — **Agreed**

**Decision:** PII in `source_payload` is **acceptable** — required for **audit trail** and **retry** of failed user syncs.

| Topic | Policy |
|-------|--------|
| Content | Directory/RC user snapshot for that operation |
| Retention | Per wiki — purge old `job` / `job_detail` after communicated period |
| Access | Admin / Service Web only; not in unstructured logs |
| Email reports | No full payload in email (next phase) |

**ADR:** [009-job-detail-source-payload-pii.md](../adr/009-job-detail-source-payload-pii.md)

---

## Implementation gate

Full checklist: [planning/implementation-gate.md](../planning/implementation-gate.md)

Before feature implementation:

- [x] Gap 1 — trigger types (Phase 1 model documented)
- [x] Gap 2 — notification (Phase 1: DSB + API; email next phase)
- [x] Gap 3 — number assignment (inventory only; no purchase)
- [x] Gap 4 — queue abstraction (ElasticMQ / SQS)
- [x] Gap 5 — directory SDK adapters (no SCIM Phase 1)
- [x] PLA API — Extensions CRUD documented; other ops stubbed
- [x] Gap 6 — directory auth port + account OAuth table (ETM deferred)
- [x] `job_detail.source_payload` PII — accepted (audit + retry); retention/access per ADR-009
- [x] AppSec — **AES-256 + deploy-time key approved** (ADR-008, ASWIP-2034)
- [x] AppSec — ADR-009 (`source_payload` PII retention/access) **approved**
- [x] PM — PRD deviations sign-off **approved** (implicit Type 2, inventory-only numbers, email deferred)
