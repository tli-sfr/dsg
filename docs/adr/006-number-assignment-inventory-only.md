# ADR-006: Number assignment — inventory only (P0-2)

**Status:** Accepted  
**Date:** 2026-05-27  
**Related:** P0-2; [gap-resolution.md](../prd/gap-resolution.md#gap-3-number-assignment-p0-2--closed-phase-1)

---

## Context

PRD P0-2 describes IDP-driven number assignment with two number sources: assign from **Number Inventory / Company Number**, or **purchase a new number**, plus On-demand country / Proof of Address (PoA) handling when purchasing.

The DSG wiki models area code selection via `dl_area_code_assignment` and `dl_area_code_type` (ADDRESS vs SPECIFIED_AREA_CODE) but does not define purchase flows.

Engineering agreed that **Phase 1 uses inventory numbers only** — no number purchase path.

---

## Decision

### Phase 1 number assignment

| PRD concept | Phase 1 behavior |
|-------------|------------------|
| Area code from IDP field | Yes — `dl_area_code_assignment` (ADDRESS or SPECIFIED_AREA_CODE) |
| Number source | **Inventory / company number only** — `number_source` fixed to `INVENTORY` (implicit; no admin choice) |
| Purchase new number | **Out of scope** — not implemented |
| On-demand country / PoA skip | **N/A** — no auto-purchase |
| Insufficient inventory | **Fail explicitly** — `job_detail` error with user identity and reason; no silent failure |

### Worker logic (provision / Type 1 only)

1. Resolve area code from IDP attributes per rule `dl_area_code_assignment`.
2. Call RC API to assign an available number from **account number inventory** matching area code / policy.
3. If no suitable inventory number: mark `job_detail` **Failed**, `comment` describes insufficient inventory (and affected user `external_id`).
4. Never invoke purchase or billing APIs.

### Schema (wiki baseline — no purchase extensions)

Use existing wiki tables:

- `dl_area_code_assignment` — linked to `provisioning_assignment_rule`
- `dl_area_code_type` — ADDRESS | SPECIFIED_AREA_CODE

No `number_source` column required in Phase 1 if inventory-only is the only mode; document in API/rule payload as optional field defaulting to `INVENTORY` for forward compatibility.

### API rule payload (Phase 1)

```json
{
  "areaCodePreference": {
    "prefer": "AreaCode",
    "areaCodeList": [
      { "countryCode": 1, "areaCode": "650" }
    ]
  }
}
```

Omit `numberSource` or send `"numberSource": "INVENTORY"` only. Reject `PURCHASE` with validation error if sent.

### Deferred to a later phase

- Purchase new number from carrier
- PoA / on-demand country notification flow
- Admin UI toggle: inventory vs purchase

---

## Consequences

### Positive

- Closes wiki/PRD gap without purchase, PoA, or billing integration
- Aligns with PRD non-goal of no auto-purchase (license); same discipline for numbers
- `dl_area_code_assignment` from wiki is sufficient for Phase 1

### Negative / PRD deviation

- PRD user story option “purchase a new number” not available until a future phase — **PM sign-off required**

### PRD acceptance criteria mapping

| Criterion | Phase 1 |
|-----------|---------|
| IDP region → area code configurable | Yes |
| Inventory assignment | Yes (only source) |
| Inventory preferred over purchase when both exist | N/A (purchase not offered) |
| PoA: skip auto-purchase, notify admin | N/A |
| Insufficient inventory: specific error per user | Yes |
| No silent failure | Yes |

---

## References

- [directory-integration-2.0.md](../prd/directory-integration-2.0.md) — P0-2
- [dsg-design-wiki.md](../architecture/dsg-design-wiki.md) — `dl_area_code_assignment`
- [003-rule-triggers-and-action-sets.md](003-rule-triggers-and-action-sets.md) — create-only
