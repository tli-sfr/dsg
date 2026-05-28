# Directory Integration 2.0 — Product Requirements

**Status:** In Review  
**Author:** Ken Wang  
**Date:** 2026-04-09  
**Team:** SW  
**Dependencies:** SW, PLA  

> Translated from product PRD into repo-local markdown. Implementation design: [architecture/overview.md](../architecture/overview.md).

---

## 1. Background and problem statement

### Why this project

RingCentral directory integration (6–8 years old) relies on third-party IDP SCIM apps (Okta, Azure AD, Google). Multi-product offerings (RingEX, RingCX, RingSense, PTT, AIR) exceed what SCIM apps support. Large enterprises (e.g. Columbia University, Waste Management) need capabilities cloud vendors will not customize. **RingCentral must host its own directory sync service.**

### Current pain points

- Sync depends entirely on third-party SCIM apps
- SCIM apps provision RingEX license users only; not other product licenses
- No bidirectional field sync
- Cannot assign multiple licenses in one provisioning event
- SE-maintained PowerShell scripts are unsustainable

### Known customers on PowerShell workarounds

John Lewis & Partners, Veolia, Nexity, Synergie, SFIL, Wilson Group Australia, Victra, Precision Software Technologies, The Stepstone Group, Coventry & Warwickshire NHS Trust.

---

## 2. Goals

| Goal | Description |
|------|-------------|
| Full MACD lifecycle | Move, Add, Change, Delete driven by directory events |
| Multi-license assignment | One provisioning event assigns multiple product licenses |
| Bidirectional field sync | RC ↔ IDP; configurable per field |
| Rule-based automation UI | No-code rules from AD events and user groups |
| Eliminate SE script dependency | Native platform; migrate customers within 6 months |
| Expanded sync fields | Site, Role, Cost Center, custom fields, etc. |
| Unassigned extension consumption | Prefer legacy unassigned extensions; auto-replenish from license inventory |
| Compatible with any SCIM IDP | Not limited to Okta/Azure/Google (P2 — design must accommodate) |

---

## 3. Non-goals

| Non-goal | Clarification |
|----------|---------------|
| Replace existing IDP connectors | Advanced option; existing SCIM integrations unchanged for simple customers |
| Auto-purchase on insufficient licenses | Fail with clear error; admin purchases manually |
| Become an IDP | Sync consumer only; read/write existing IDPs |

---

## 4. User stories (summary)

### New user provisioning

- **Field sync:** Site, Role, Cost Center, custom fields; configurable mapping (static, pass-through, conditional)
- **Phone numbers:** Area code from IDP field; **Phase 1: inventory / company number only** (no purchase); explicit errors on insufficient inventory — see [ADR-006](../adr/006-number-assignment-inventory-only.md)
- **Multi-license:** RingEX, RingCX, add-ons in one step (P1)
- **Unassigned extensions:** Consume first; then license inventory (P1)
- **License errors:** Clear failure list; no auto-purchase
- **Templates:** Auto-apply user and call-handling templates after create

### User deprovision / delete

Pre-configured strategy when IDP deletes user:

| Option | Behavior |
|--------|----------|
| **A — Complete delete** | Delete extension; reclaim number and license |
| **B — Delete extension, retain resources** | Extension deleted; number and license returned to inventory |
| **C — Disable only** | Extension disabled; number and license stay assigned |

### User data change sync (IDP ↔ RC)

- RC number/extension changes push to IDP fields (P0-3)
- IDP changes matching rules update RC fields (P0-6)

### Sync result reporting

- After each job: success count, failed users with reasons; email delivery (P0-9)

---

## 5. Functional requirements

### P0 — Customer commitments (Phase 1, target 2026 Q3)

| ID | Requirement | Acceptance criteria (summary) |
|----|-------------|-------------------------------|
| **P0-1** | Extended field sync and attribute mapping | Site, Role, Cost Center, custom fields; static, pass-through, conditional mapping in UI |
| **P0-2** | IDP-driven number assignment | Configurable region→area code; inventory vs purchase; PoA skip + notify; explicit inventory errors |
| **P0-3** | RC → IDP reverse field sync | RC number/extension changes update IDP; idempotent; alert on failure |
| **P0-4** | Auto-apply user and call-handling templates | Post-create apply; user still created if template fails; admin notified |
| **P0-5** | Direct device assignment | Softphone, BYOD, hardphone from inventory; errors on insufficient inventory |
| **P0-6** | IDP data changes trigger RC rules | Matching rules update RC; no match = no change; results in report |
| **P0-7** | Three deprovision strategies | Options A/B/C configurable; results in report |
| **P0-8** | Rules table — three trigger types | Type 1 provision, Type 2 change, Type 3 delete; priority; enable/disable |

**Phase 1 implementation note (P0-6 / P0-8):** Type 2 (change) is **implicit**—updates occur when basic/custom attribute mapping values change after directory fetch, not via separate update rules. Type 1 and Type 3 remain explicit. See [gap-resolution.md](gap-resolution.md#gap-1-three-trigger-types-p0-8-p0-6--closed-phase-1) and [ADR-003](../adr/003-rule-triggers-and-action-sets.md).
| **P0-9** | Sync result report notification | Email after job; success count; per-user failure reasons |

### P1 — Multi-product support (Phase 2)

| ID | Requirement |
|----|-------------|
| **P1-1** | Multi-license one-time assignment (atomic) |
| **P1-2** | Clear error on insufficient license; no auto-purchase; per-user failure in batch |
| **P1-3** | Unassigned extension priority + auto-replenishment |

### P2 — Future flexibility (design only, not implemented)

| ID | Requirement |
|----|-------------|
| **P2-1** | Generic SCIM IDP via configuration |
| **P2-2** | External provisioning API (ServiceNow, Workday) |
| **P2-3** | Sync result Web UI dashboard and failed-user re-sync |

---

## 6. Success metrics

| Type | Metric | Target |
|------|--------|--------|
| Leading (30–60 days) | Active accounts with rules configured | Committed customers within 60 days |
| Leading (30–60 days) | Provisioning success rate | ≥ 95% (excluding expected license shortages) |
| Lagging (60–180 days) | Provisioning-related tickets | PowerShell ticket demand reduced 50% |

---

## 7. Open questions

| Owner | Status | Question |
|-------|--------|----------|
| Engineering | Blocking | Okta/Azure/Google write-back APIs, rate limits, consistency |
| PM | Blocking | Legal multi-license combinations (e.g. RingCX requires RingEX) |
| PM | Open | Conflict resolution: last-write-wins vs per-field source priority |
| Engineering | Open | Auth model for IDP write-back (OAuth vs service account) |
| Engineering | Open | Throughput for 10k+ users; queue strategy |

Local ADRs document **provisional** resolutions where planning must proceed; see [gap-resolution.md](gap-resolution.md).

---

## 8. Dependencies

| Dependency | Responsibility | Deliverable |
|------------|----------------|-------------|
| SW | Admin Portal UI, rules engine, sync service | Rule UI, event processor |
| PLA | RC API expansion | Phase 1: [Extensions CRUD](https://developers.ringcentral.com/api-reference/Extensions/createExtension); site/role/cost center/etc. stubbed — [rc-pla-api-spec.md](../api/rc-pla-api-spec.md) |

---

## 9. Delivery phases

**Phase 1 (P0 core):** P0-1 through P0-8 — target 2026 Q3  

**Phase 1 local deviations (documented):**

- **P0-9:** Job report **data and API** in Phase 1; **email notification** in the **next phase** — [ADR-005](../adr/005-sync-report-notification.md).
- **P0-2:** **Inventory-only** number assignment; purchase and PoA flows in a **later phase** — [ADR-006](../adr/006-number-assignment-inventory-only.md).
- **P2-1 / IDP access:** **Vendor directory SDKs** (Azure, Okta, Google, OneLogin); **no SCIM** in Phase 1 — [ADR-004](../adr/004-directory-sdk-adapters.md).

**Next phase (after P0 core):** P0-9 email delivery; optional P0-2 purchase/PoA; optional P2-1 SCIM adapter; then P1 (P1-1, P1-2, P1-3)
