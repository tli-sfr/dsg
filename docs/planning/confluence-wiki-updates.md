# Proposed Confluence wiki updates (page 914831306)

Submit these changes to the DSG architectural wiki after local validation sign-off. Source: [wiki-validation.md](../prd/wiki-validation.md).

---

## 1. New subsection: PRD alignment

**Location:** Top of page or §1 Introduction

**Content:** Link to traceability table (export from [traceability.md](../prd/traceability.md)) with columns: PRD ID, Wiki coverage, Phase 1 status, ADR link.

**Rationale:** Single place for PM/engineering to see closed vs deferred items.

---

## 2. Rule trigger types

**Location:** §2.4 ER diagram + `provisioning_assignment_rule` description

**Add:**

- Type 1 **Provision** — `provisioning_assignment_rule` + `selection_expression`
- Type 2 **Change** — implicit via `attribute_mapping` / `custom_attribute_mapping` (Phase 1); optional future update rules
- Type 3 **Delete** — `deprovisioning_rule` / `deprovisioning_type`

Reference RingCentral internal ADR-003 equivalent or link to repo `docs/adr/003-rule-triggers-and-action-sets.md`.

---

## 3. P0-9 notification flow

**Location:** §3.3 Job state machine (after consolidation step)

**Add diagram:**

- Phase 1: consolidation → report query API
- Phase 2: `ReportNotifier` → email template (success count, failure table)

Clarify email is **not** Phase 1 GA requirement.

---

## 4. Number assignment extension

**Location:** §2.4 / worker §3.5

**Extend beyond `dl_area_code_assignment`:**

| Phase 1 | Later phase |
|---------|-------------|
| Inventory / company number only | Purchase new number |
| Fail with explicit error | PoA / on-demand country handling |

Link to inventory-only product decision.

---

## 5. Queue strategy (ElasticMQ / SQS)

**Location:** §5 Queue design

**Add:**

- Local dev: ElasticMQ (SQS-compatible API)
- Production: AWS SQS
- Application uses `MessageQueuePort`; no dual code paths

Update cost section footnote: local dev does not use AWS SQS charges.

---

## 6. Monitoring (fill §3.3 empty section)

**Metrics:**

| Metric | Purpose |
|--------|---------|
| `sync_job_completed_total` | Success rate (PRD ≥95%) |
| `job_detail_failed_total` | Failure diagnostics |
| `job_stuck_count` | Stuck jobs |
| `idp_429_retry_total` | Rate limit health |
| `reverse_sync_push_failed_total` | P0-3 |

Structured logging: `account_id`, `job_id`, `job_detail_id`, `external_id`.

---

## 7. On-demand sync (close 2.3.4.3 TBD)

**Decision:**

- ON_DEMAND job type for manual sync, preview, selected users, failed-user retry
- Per-account mutex: no overlapping FULL / INCREMENTAL / ON_DEMAND
- Tie to P2-3 dashboard as UI enhancement, not blocker for API

---

## 8. Authentication (ETM deferred path)

**Add Phase 1 note:**

- `account_directory_oauth` when ETM unavailable
- `DirectoryAuthPort` abstraction
- AES-256 for secrets at deploy (AppSec ASWIP-2034)

Keep ETM as future profile.

---

## Submission checklist

- [ ] Arch review scheduled
- [ ] PM acknowledges PRD deviations (Type 2 implicit, email deferred, inventory-only numbers)
- [ ] PLA review stub section for non-Extensions APIs
- [ ] AppSec review PII / crypto sections
