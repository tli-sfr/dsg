# Implementation gate checklist

Do **not** start feature implementation until all **required** items are checked. Tracks the validation plan Step 6 and [gap-resolution.md](../prd/gap-resolution.md#implementation-gate).

**Last updated:** 2026-05-28

---

## ADRs and design

| Item | Status | Reference |
|------|--------|-----------|
| ADR-001 Event model (scheduled vs SCIM) | Done | [001-event-model.md](../adr/001-event-model.md) |
| ADR-002 Queue abstraction | Done | [002-queue-abstraction.md](../adr/002-queue-abstraction.md) |
| ADR-003 Rule triggers + action sets | Done | [003-rule-triggers-and-action-sets.md](../adr/003-rule-triggers-and-action-sets.md) |
| ADR-004 IDP adapters (no SCIM P1) | Done | [004-directory-sdk-adapters.md](../adr/004-directory-sdk-adapters.md) |
| ADR-010 Conflict / idempotency (P0-3) | Proposed | [010-p0-3-idempotency-and-conflict.md](../adr/010-p0-3-idempotency-and-conflict.md) |
| Wiki validation signed | Done | [wiki-validation.md](../prd/wiki-validation.md) |

---

## Contracts and schema

| Item | Status | Reference |
|------|--------|-----------|
| OpenAPI v0.1 | Done | [dsg-openapi.yaml](../api/dsg-openapi.yaml) |
| ERD reviewed | Done | [erd.md](../db/erd.md) |
| `selection_expression` schema documented | Done | [selection-criteria.md](../architecture/selection-criteria.md) |
| PLA Extensions API + stubs documented | Done | [rc-pla-api-spec.md](../api/rc-pla-api-spec.md) |

---

## Local environment

| Item | Status | Reference |
|------|--------|-----------|
| `docker compose` MySQL + ElasticMQ | Done | [docker-compose.yml](../../docker-compose.yml) |
| ElasticMQ queue names configured | Done | [config/elasticmq.conf](../../config/elasticmq.conf) |
| SPIKE-4 POC (queue listeners) | **Done** | [SPIKE-4-elasticmq-poc.md](../spikes/SPIKE-4-elasticmq-poc.md) |

---

## Security and compliance

| Item | Status | Reference |
|------|--------|-----------|
| AppSec AES-256 + deploy key | Done | ADR-008, ASWIP-2034 |
| AppSec ADR-009 PII retention | **Done** (approved) | [009-job-detail-source-payload-pii.md](../adr/009-job-detail-source-payload-pii.md) |
| PM sign-off PRD deviations | **Done** (approved) | [gap-resolution.md](../prd/gap-resolution.md) |

---

## First implementation slice (post-gate)

**Scope:** No live RC or IDP calls.

1. Maven multi-module skeleton (`dsg-api`, `dsg-worker`, `dsg-domain`)
2. Flyway migrations from [dsb-schema-wiki.md](../db/dsb-schema-wiki.md) + auth extension
3. `MessageQueuePort` + ElasticMQ wiring (after SPIKE-4)
4. Job Manager: create `job`, publish to queue, per-account mutex (`409`)
5. Stub consolidator marking job terminal

**Out of scope for slice 1:** Rules engine evaluation, Extensions API, directory pull, admin UI.

---

## Gate verdict

| Verdict | Condition |
|---------|-----------|
| **Planning complete** | All ADR rows Done/Proposed; OpenAPI + ERD Done |
| **Ready for slice 1** | **Yes** — SPIKE-4 done; run `mvn -pl dsg-messaging verify` locally |
| **Ready for E2E sync** | Slice 1 + Epic 4 adapters + PLA live/stub paths |
