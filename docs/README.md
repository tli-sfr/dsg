# Directory Sync Gateway (DSG) — Documentation

Local source of truth for **Directory Integration 2.0** (PRD) and **DSG architectural design** (wiki), reconciled for implementation.

## Quick links

### Product and validation

| Document | Description |
|----------|-------------|
| [prd/directory-integration-2.0.md](prd/directory-integration-2.0.md) | Product PRD summary |
| [prd/wiki-validation.md](prd/wiki-validation.md) | **Wiki validation workshop** — gaps, owners, decisions |
| [prd/traceability.md](prd/traceability.md) | PRD ↔ design matrix |
| [prd/gap-resolution.md](prd/gap-resolution.md) | Gap decisions (Phase 1) |
| [prd/license-matrix.md](prd/license-matrix.md) | License combos (PM placeholder) |

### Architecture

| Document | Description |
|----------|-------------|
| [architecture/dsg-design-wiki.md](architecture/dsg-design-wiki.md) | Engineering wiki summary |
| [architecture/overview.md](architecture/overview.md) | Unified architecture |
| [architecture/sync-runtime.md](architecture/sync-runtime.md) | Job pipeline, mutex, job types |
| [architecture/selection-criteria.md](architecture/selection-criteria.md) | Rule trigger AND criteria → JSON |

### Planning

| Document | Description |
|----------|-------------|
| [planning/implementation-gate.md](planning/implementation-gate.md) | Pre-implementation checklist |
| [planning/spike-tickets.md](planning/spike-tickets.md) | Spike definitions |
| [planning/jira-epics.md](planning/jira-epics.md) | Jira epic/story structure |
| [planning/confluence-wiki-updates.md](planning/confluence-wiki-updates.md) | Proposed Confluence edits |

### ADRs

| ADR | Topic |
|-----|--------|
| [001](adr/001-event-model.md) | Scheduled jobs vs SCIM |
| [002](adr/002-queue-abstraction.md) | ElasticMQ / SQS |
| [003](adr/003-rule-triggers-and-action-sets.md) | Type 1 / implicit Type 2 / Type 3 |
| [004](adr/004-directory-sdk-adapters.md) | Directory SDKs; not SCIM P1 |
| [005](adr/005-sync-report-notification.md) | Report API; email next phase |
| [006](adr/006-number-assignment-inventory-only.md) | Inventory-only numbers |
| [007](adr/007-rc-pla-provisioning-port.md) | Extensions CRUD + stubs |
| [008](adr/008-directory-auth-port.md) | `DirectoryAuthPort` + DSB OAuth |
| [009](adr/009-job-detail-source-payload-pii.md) | PII in `source_payload` |
| [010](adr/010-p0-3-idempotency-and-conflict.md) | P0-3 idempotency + conflict |

### API and UI

| Document | Description |
|----------|-------------|
| [api/dsg-openapi.yaml](api/dsg-openapi.yaml) | OpenAPI v0.1 |
| [api/dsg-admin-api-spec.md](api/dsg-admin-api-spec.md) | Admin API narrative |
| [api/directory-auth-api-spec.md](api/directory-auth-api-spec.md) | IDP OAuth config |
| [api/rc-oauth-dev-setup.md](api/rc-oauth-dev-setup.md) | RC 3LO local dev |
| [api/rc-pla-api-spec.md](api/rc-pla-api-spec.md) | RC/PLA HTTP contract |
| [ui/mockup-alignment.md](ui/mockup-alignment.md) | UX mockup → routes/API |

### Database

| Document | Description |
|----------|-------------|
| [db/dsb-schema-wiki.md](db/dsb-schema-wiki.md) | All 29 wiki tables |
| [db/erd.md](db/erd.md) | ER diagram (Mermaid) |
| [db/erd-from-wiki.mmd](db/erd-from-wiki.mmd) | ER source for tooling |
| [db/schema-auth-extensions.md](db/schema-auth-extensions.md) | `account_directory_oauth` |

## Stack

React + TypeScript + Tailwind · Java + Spring Boot · MySQL · **ElasticMQ (local)** / SQS (prod)

```bash
docker compose up -d   # MySQL + ElasticMQ
mvn -pl dsg-messaging verify   # SPIKE-4 queue POC (Testcontainers)
```

**SPIKE-4:** [spikes/SPIKE-4-elasticmq-poc.md](spikes/SPIKE-4-elasticmq-poc.md)

## Sources

- PRD: `directory-integration-2.0-prd.docx.pdf` (2026-04-09)
- Wiki: [Confluence 914831306](https://wiki.ringcentral.com/spaces/Arch/pages/914831306)
