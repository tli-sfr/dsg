# ADR-010: P0-3 idempotency and conflict resolution (Phase 1 baseline)

**Status:** Proposed (planning) — implements plan **ADR-005** topic (conflict + idempotent RC→IDP push)  
**Date:** 2026-05-27  
**Related:** P0-3; [traceability.md](../prd/traceability.md); [wiki-validation.md](../prd/wiki-validation.md) G8, G9

---

## Context

PRD P0-3 requires **RC → IDP reverse sync** with **idempotent** updates and **alerts** on push failure. The wiki defines `sync_direction` RC→DIR and hash optimization but does not specify:

- Idempotency keys for outbound IDP writes
- Field-level conflict when both sides changed
- Alerting integration

---

## Decision (Phase 1 baseline)

### Idempotency (RC → IDP)

1. Each reverse-sync `job_detail` carries `idempotency_key` = hash(`account_id`, `external_id`, `operation`, `payload_version`) — **schema extension TBD** in Flyway.
2. Before IDP PATCH/PUT, worker checks `directory_sync_user_hash.rc_hash`; if payload unchanged since last successful push → **skip** (no-op success).
3. On IDP **409/412** conflict response → mark detail **Failed** with `comment` = provider error; increment `reverse_sync_push_failed_total` metric.
4. **Retry:** manual on-demand only for failed reverse-sync details in Phase 1 (same as forward sync retry pattern).

### Conflict policy (default)

| Scenario | Policy |
|----------|--------|
| Forward sync (IDP → RC), mapped field changed in IDP only | **IDP wins** — apply to RC |
| Forward sync, no mapped change | Skip |
| Reverse sync (RC → IDP), mapped field changed in RC only | **RC wins** — push to IDP if reverse mapping enabled |
| Both changed since last sync | **IDP wins on forward job**; **RC wins on reverse job** — no automatic merge |
| Unmapped fields | Ignored |

Product may override per-account later; Phase 1 uses **directional winner** to avoid blocking delivery.

### Alerting

| Event | Phase 1 | Next phase |
|-------|---------|------------|
| Reverse push failed | Structured log + metric + `job_detail.comment` | PagerDuty / email threshold |
| Stuck job | `job_stuck_count` metric | Automated alert |

---

## Open items (spike required)

- [ ] IDP write-back feasibility per vendor (Okta PATCH, Azure, Google) — [spike-tickets.md](../planning/spike-tickets.md)
- [ ] `idempotency_key` column or side table design
- [ ] AppSec review for storing IDP write payloads in `source_payload`

---

## Consequences

- Unblocks P0-3 implementation planning without waiting for full PRD conflict workshop
- Explicit **non-merge** policy reduces scope; PM must accept or amend before GA

---

## References

- [ADR-003](003-rule-triggers-and-action-sets.md) — forward update path
- [ADR-004](004-directory-sdk-adapters.md) — `DirectoryPort` write methods
