# DSG design wiki validation — workshop record

Formal validation of the [DSG architectural wiki](../architecture/dsg-design-wiki.md) against [Directory Integration 2.0 PRD](directory-integration-2.0.md) and the team stack. This document is the signed gap list and decision log from the planning workshop.

**Workshop date:** 2026-05-27 (planning phase)  
**Sources:** Confluence page [914831306](https://wiki.ringcentral.com/spaces/Arch/pages/914831306) (PDF export), local PRD summary, gap resolutions, ADRs 001–010.

---

## Executive summary

| Verdict | Detail |
|---------|--------|
| **Foundation** | Wiki is a solid basis for scheduled, batch directory sync (job pipeline, DSB schema, 429 handling, bidirectional mapping). |
| **PRD alignment (Phase 1)** | ~70% of P0 covered; critical gaps **closed in local ADRs** (triggers, notification data, numbers, queues, IDP SDKs, auth). |
| **Remaining open** | P0-3 idempotency detail, P1-1 atomic multi-license, P1-3 unassigned extension, P2 SCIM generic layer, conflict policy refinement. |
| **Stack** | ElasticMQ local + SQS prod; MySQL DSB; React admin UI; Java Spring Boot workers. |

---

## Workshop decisions (confirmed)

### Queue technology (Gap 4)

| Question | Decision | Owner |
|----------|----------|-------|
| Local dev queues? | **ElasticMQ** (`docker compose up`) | Backend |
| Production queues? | **AWS SQS** (same SQS-compatible API via `MessageQueuePort`) | Platform / Backend |
| Application code branches on vendor? | **No** — only `MessageQueuePort` | Backend |

**ADR:** [002-queue-abstraction.md](../adr/002-queue-abstraction.md)

### UI embedding model

| Mode | Phase 1 | Notes |
|------|---------|-------|
| **Production target** | Embedded in **Service Web** (SCL → JWL → DSG APIs) | Matches wiki admin path |
| **Local standalone dev** | React app + **RC 3-legged OAuth** + DSG APIs | `/etc/hosts` hostname for OAuth redirect — [rc-oauth-dev-setup.md](../api/rc-oauth-dev-setup.md) |
| **IDP credentials** | Configured via DSG admin API / UI card (not RC 3LO) | [ADR-008](../adr/008-directory-auth-port.md) |

**Owner:** Frontend + Arch

### Event model (scheduled vs SCIM)

| Question | Decision | Owner |
|----------|----------|-------|
| Phase 1 ingestion? | **Scheduled + on-demand jobs** pulling directory APIs (Azure Graph, Okta, Google, OneLogin SDKs) | Backend |
| SCIM webhooks? | **Deferred** (P2-1); adapter interface allows future SCIM consumer | Arch |

**ADR:** [001-event-model.md](../adr/001-event-model.md)

### Rule trigger types (P0-8 / P0-6)

| Type | Phase 1 | Owner |
|------|---------|-------|
| Type 1 Provision | Explicit `provisioning_assignment_rule` + selection criteria | Backend + Frontend |
| Type 2 Change | **Implicit** via `attribute_mapping` / `custom_attribute_mapping` hash compare | Backend |
| Type 3 Delete | `deprovisioning_rule` policy | Backend + Frontend |

**ADR:** [003-rule-triggers-and-action-sets.md](../adr/003-rule-triggers-and-action-sets.md)

---

## Gap register with owners

| ID | Gap | Wiki state | Resolution | Owner | Status |
|----|-----|------------|------------|-------|--------|
| G1 | Three trigger types | Rules only | ADR-003 Phase 1 model | Eng + PM | **Closed** |
| G2 | P0-9 email notification | Data only | Phase 1 report API; email next phase — ADR-005 | Backend | **Closed (partial)** |
| G3 | P0-2 purchase / PoA | Area code only | Inventory-only Phase 1 — ADR-006 | PM + Backend | **Closed (partial)** |
| G4 | ElasticMQ vs SQS | SQS in wiki | ADR-002 abstraction | Backend | **Closed** |
| G5 | SCIM vs per-IDP APIs | Per-IDP tables | SDK adapters Phase 1 — ADR-004 | Backend | **Closed (partial)** |
| G6 | ETM unavailable | ETM in wiki | DSB OAuth Phase 1 — ADR-008 | Backend + AppSec | **Closed** |
| G7 | PLA APIs incomplete | Assumed live | Extensions CRUD + stubs — ADR-007 | PLA + Backend | **Closed (partial)** |
| G8 | P0-3 idempotency + alerts | RC→DIR flow only | ADR-010 spike + schema TBD | Backend | **Open** |
| G9 | Conflict resolution | Hash skip only | ADR-010 default policy | Arch | **Open** |
| G10 | P1-1 multi-license atomicity | Multiple license rows | PLA spike + ADR-007 stub | PLA | **Open** |
| G11 | P1-3 unassigned extension | Missing | PLA dependency | PLA | **Open** |
| G12 | On-demand / failed-user retry | Wiki TBD | Documented in [sync-runtime.md](../architecture/sync-runtime.md) | Backend | **Closed** |
| G13 | Monitoring §3.3 empty | Empty | Metrics in [overview.md](../architecture/overview.md) | SRE + Backend | **Closed (local)** |

---

## PRD traceability snapshot

Full matrix: [traceability.md](traceability.md).

| PRD ID | Status |
|--------|--------|
| P0-1 | Closed |
| P0-2 | Closed (inventory only) |
| P0-3 | Partial — ADR-010 |
| P0-4–P0-8 | Closed (Phase 1 scope) |
| P0-9 | Closed partial (no email Phase 1) |
| P1-1–P1-3 | Partial / Open |
| P2-1–P2-3 | Partial / Open |

---

## ADR index (plan mapping)

| Plan ADR | Topic | Local document |
|----------|--------|----------------|
| ADR-001 | Event model (scheduled vs SCIM) | [001-event-model.md](../adr/001-event-model.md) |
| ADR-002 | Queue abstraction | [002-queue-abstraction.md](../adr/002-queue-abstraction.md) |
| ADR-003 | Rule triggers + action sets | [003-rule-triggers-and-action-sets.md](../adr/003-rule-triggers-and-action-sets.md) |
| ADR-004 | IDP adapter interface | [004-directory-sdk-adapters.md](../adr/004-directory-sdk-adapters.md) |
| ADR-005 (plan) | Conflict + idempotency | [010-p0-3-idempotency-and-conflict.md](../adr/010-p0-3-idempotency-and-conflict.md) |
| — | Sync report (plan used 005 for conflict) | [005-sync-report-notification.md](../adr/005-sync-report-notification.md) |
| — | Numbers, PLA, auth, PII | ADR-006–009 |

---

## Wiki update package

Confluence change proposals: [../planning/confluence-wiki-updates.md](../planning/confluence-wiki-updates.md).

---

## Next actions

1. PM sign-off on PRD deviations (implicit Type 2, inventory-only numbers, email deferred).
2. Execute spikes: [../planning/spike-tickets.md](../planning/spike-tickets.md).
3. Jira epics: [../planning/jira-epics.md](../planning/jira-epics.md).
4. Implementation gate: [../planning/implementation-gate.md](../planning/implementation-gate.md).

---

## Attendees (record)

| Role | Responsibility in validation |
|------|------------------------------|
| Architecture | Wiki fidelity, ADR approval |
| Backend | Queue, job pipeline, adapters, API |
| Frontend | UI embedding, mockup alignment, OAuth dev |
| PM | PRD deviations, license matrix |
| PLA | Extensions + stub contracts |
| AppSec | ASWIP-2034, PII retention (ADR-008, ADR-009) |
