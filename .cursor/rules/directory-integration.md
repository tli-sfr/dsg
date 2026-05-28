# Directory Integration 2.0 / DSG — Cursor rules

## Project

Directory Sync Gateway (DSG) implements **Directory Integration 2.0** for RingCentral: sync corporate directory users to RC extensions with rules, mappings, and job-based orchestration.

## Stack (fixed)

- **Backend:** Java 21+, Spring Boot, Maven multi-module (`dsg-api`, `dsg-worker`, `dsg-domain`)
- **DB:** MySQL (DSB), Flyway migrations from `docs/db/dsb-schema-wiki.md`
- **Queues:** ElasticMQ locally; AWS SQS in production — **always** use `MessageQueuePort` ([ADR-002](../../docs/adr/002-queue-abstraction.md))
- **Frontend:** React, TypeScript, Tailwind (Service Web embed + standalone local dev with RC 3LO)

## Planning sources (read before coding)

| Doc | Use |
|-----|-----|
| [docs/prd/gap-resolution.md](../../docs/prd/gap-resolution.md) | Closed gap decisions |
| [docs/prd/traceability.md](../../docs/prd/traceability.md) | PRD ↔ implementation |
| [docs/prd/wiki-validation.md](../../docs/prd/wiki-validation.md) | Wiki validation + owners |
| [docs/adr/003-rule-triggers-and-action-sets.md](../../docs/adr/003-rule-triggers-and-action-sets.md) | **Type 1 explicit rules; Type 2 implicit mapping; Type 3 deprovision** |
| [docs/api/dsg-openapi.yaml](../../docs/api/dsg-openapi.yaml) | Admin API contract v0.1 |

## Non-goals (Phase 1)

- **No SCIM protocol** — vendor SDK adapters only ([ADR-004](../../docs/adr/004-directory-sdk-adapters.md))
- **No update provisioning rules** — changes via `attribute_mapping` / `custom_attribute_mapping` hash only
- **No phone number purchase / PoA** — inventory only ([ADR-006](../../docs/adr/006-number-assignment-inventory-only.md))
- **No sync completion email** — report API only; email next phase ([ADR-005](../../docs/adr/005-sync-report-notification.md))
- **No ETM dependency** — `DirectoryAuthPort` + DSB OAuth ([ADR-008](../../docs/adr/008-directory-auth-port.md))

## RC / PLA integration

- **Live:** RingCentral Extensions API for extension CRUD
- **Stub:** site, role, cost center, phone assign, templates, devices, multi-license — `RcProvisioningPort` ([ADR-007](../../docs/adr/007-rc-pla-provisioning-port.md))

## Job orchestration

- Per-account **mutex**: only one non-terminal job per account ([sync-runtime.md](../../docs/architecture/sync-runtime.md))
- Pipeline: Job Manager → job queue → retrieval publisher → detail queue → worker → consolidator
- `job_detail.source_payload` may contain PII for audit/retry ([ADR-009](../../docs/adr/009-job-detail-source-payload-pii.md))

## Implementation gate

Do not start feature work until [implementation-gate.md](../../docs/planning/implementation-gate.md) checklist passes. First slice: Job Manager + DSB migrations + queue skeleton (no live RC/IDP).

## Code style

- Match existing module conventions; minimal diffs
- No secrets in repo; use env / deploy-time keys for `dsg.crypto.secret-key`
- Prefer port interfaces (`DirectoryPort`, `DirectoryAuthPort`, `MessageQueuePort`, `RcProvisioningPort`) over direct vendor SDK calls in workers

## Local dev

```bash
docker compose up -d   # MySQL + ElasticMQ
```
