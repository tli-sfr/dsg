# ADR-009: PII in `job_detail.source_payload`

**Status:** Accepted  
**Date:** 2026-05-27  
**Related:** AppSec (ASWIP-2034); wiki `job_detail`; [gap-resolution.md](../prd/gap-resolution.md#data-handling-job_detailsource_payload)

---

## Context

`job_detail` (wiki section 2.4.2) includes `source_payload` (varchar/json) — snapshot of user data from the directory or RC at sync time. This may contain **PII** (name, email, phone, department, etc.).

AppSec review may question storing PII in DSB.

---

## Decision

**Storing PII in `job_detail.source_payload` is acceptable and required for Phase 1.**

| Purpose | Why payload is needed |
|---------|------------------------|
| **Audit trail** | Reconstruct what directory/RC data was processed for a given sync operation |
| **Retry** | Re-run failed user sync without re-fetching directory (e.g. after RC validation fix) |
| **Failure diagnosis** | Compare payload to error in `comment` for admin support |

### What we store

- Directory or RC user JSON as returned for that `job_detail` operation (bounded by column size; wiki `varchar(2048)` — consider `TEXT` if payloads exceed limit in implementation)
- Same data used for hash / mapping; not an additional PII collection beyond sync scope

### Controls (still required)

| Control | Requirement |
|---------|-------------|
| **Access** | Job report APIs restricted to account admin / Service Web authz |
| **Retention** | Apply wiki data retention policy — purge `job` / `job_detail` older than communicated period |
| **EU** | EU accounts use EU DSB instance (wiki compliance) |
| **Logs** | Do **not** duplicate full `source_payload` in application logs (Kibana); use `job_detail_id` reference |
| **Email reports** (next phase) | Summarize failures (email + reason); avoid attaching full payload in email |

### What this does not approve

- Storing OAuth secrets in `source_payload` (secrets stay in `account_directory_oauth` encrypted columns only)
- Indefinite retention without policy
- Exposing payload to unauthenticated callers

---

## Consequences

- AppSec review can mark `source_payload` PII as **accepted use case** with retention + access controls
- Retry and on-demand re-sync flows (wiki 2.3.4.3) can rely on persisted payload
- Implementation should monitor payload size; escalate column type if 2048 is insufficient

---

## References

- [dsg-design-wiki.md](../architecture/dsg-design-wiki.md) — `job_detail` table
- [005-sync-report-notification.md](005-sync-report-notification.md)
