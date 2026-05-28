# PRD traceability matrix

Maps product requirements to wiki design and local implementation decisions.

**Validation workshop:** [wiki-validation.md](wiki-validation.md)

**Legend:** Closed = gap resolved in local docs; Partial = wiki + extension needed; Open = not yet designed.

| PRD ID | Requirement | Wiki coverage | Local status | Notes |
|--------|-------------|---------------|--------------|-------|
| P0-1 | Extended field sync & mapping | `attribute_mapping`, `custom_attribute_mapping`, `rule_based_attribute_mapping` | Closed | Conditional mapping via `selection_expression` on **create** |
| P0-2 | IDP-driven number assignment | `dl_area_code_assignment` | **Closed (Phase 1)** | **Inventory only**; no purchase/PoA — [ADR-006](../adr/006-number-assignment-inventory-only.md) |
| P0-3 | RC → IDP reverse sync | `sync_direction` RC→DIR | Partial | Idempotency TBD; tokens via [ADR-008](../adr/008-directory-auth-port.md) |
| P0-4 | Auto-apply templates | `template_assignment` | Closed | Create only; async failure handling |
| P0-5 | Device assignment | `device_assignment` | Closed | Create only |
| P0-6 | IDP change → RC update | Hash + incremental | **Closed** | [ADR-003](../adr/003-rule-triggers-and-action-sets.md): mapping delta only |
| P0-7 | Deprovision strategies | `deprovisioning_type` | Closed | Maps to PRD options A/B/C |
| P0-8 | Rules table three triggers | `provisioning_assignment_rule` | **Closed (Phase 1)** | Type 2 implicit; see [gap-resolution.md](gap-resolution.md#gap-1-three-trigger-types-p0-8-p0-6--closed-phase-1) |
| P0-9 | Sync report notification | `job`, `job_detail` | **Closed (Phase 1 partial)** | Phase 1: DSB + report API; **email in next phase** — [ADR-005](../adr/005-sync-report-notification.md) |
| P1-1 | Multi-license atomic assign | `license_assignment` | Partial | PLA API + atomicity ADR |
| P1-2 | No auto-purchase | Error logging | Partial | Explicit error codes TBD |
| P1-3 | Unassigned extension priority | — | Open | PLA dependent |
| P2-1 | Generic SCIM IDP | Per-IDP APIs | **Closed (Phase 1 partial)** | Phase 1: vendor **SDKs** only — [ADR-004](../adr/004-directory-sdk-adapters.md); generic SCIM deferred |
| P2-2 | External provisioning API | — | Open | Extension point only |
| P2-3 | Sync dashboard UI | Stuck job mention | Open | Phase 2 |

---

## P0-6 / P0-8 acceptance criteria mapping (Phase 1)

| Acceptance criterion | How met in Phase 1 |
|----------------------|-------------------|
| When rule conditions match, RC fields updated | **Create:** `provisioning_assignment_rule` + mappings. **Update:** mapping field delta only |
| When no rules matched, RC not modified | **Update:** no mapped field change → skip. **Create:** no matching provision rule → per product default (document in worker) |
| Rule execution in sync report | `job_detail` with `operation`, `comment` |
| Three trigger types independently configured | Type 1 & 3 explicit rules; Type 2 = mapping config (implicit) |
| Type 1 conditional logic (e.g. AD group) | `selection_expression` on provision rules |
| Multiple rules same event by priority | `provisioning_assignment_rule.priority` on **create** only |

---

## P0-9 acceptance criteria mapping

| Acceptance criterion | Phase 1 | Next phase |
|----------------------|---------|------------|
| Report triggered after job completion | Job consolidation writes terminal state | Same |
| Success and failure records | `job_detail` states + counts | Same |
| Failed entries identify user and reason | `external_id`, `comment` | Same |
| Report delivered via email | **Deferred** | Email notifier worker |
| Admin can review without email | Report query API + Service Web | Optional dashboard (P2-3) |

---

## P0-2 acceptance criteria mapping (Phase 1)

| Acceptance criterion | Phase 1 |
|----------------------|---------|
| IDP region → area code mapping configurable | Yes — `dl_area_code_assignment` |
| Assign from number inventory | Yes — **only** source |
| Purchase new number | Deferred |
| PoA / on-demand skip auto-purchase | N/A |
| Insufficient inventory: specific error, affected users | Yes — `job_detail.comment` |
| No silent failure | Yes |
