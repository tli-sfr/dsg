# Jira backlog structure — DSG

Epics derived from wiki components and PRD acceptance criteria. Copy stories into Jira with links to `docs/` sections.

---

## Epic 1: DSB schema + migrations

**Wiki:** §2.4 Database design  
**Docs:** [dsb-schema-wiki.md](../db/dsb-schema-wiki.md), [schema-auth-extensions.md](../db/schema-auth-extensions.md)

| Story | Acceptance criteria |
|-------|---------------------|
| Flyway baseline (29 wiki tables) | All tables created; FKs match wiki |
| Auth extension `account_directory_oauth` | Matches ADR-008 |
| Seed reference data (`directory_type`, `license_type`, …) | Lookup tables populated |
| ER reviewed with arch | Sign-off in wiki-validation |

---

## Epic 2: DSG Admin API

**Wiki:** §2.6 APIs  
**Docs:** [dsg-openapi.yaml](../api/dsg-openapi.yaml), [dsg-admin-api-spec.md](../api/dsg-admin-api-spec.md)

| Story | Acceptance criteria |
|-------|---------------------|
| Directory CRUD + group scope | POST/PUT/GET `/directory` |
| Directory OAuth config + test | PUT/GET/POST test per ADR-008 |
| Attribute mapping API | Basic + custom mappings persisted |
| Provisioning rule API | Rule + assignments + `selection_expression` |
| Job trigger + report API | POST `/jobs`, GET report; 409 on concurrent job |
| OpenAPI v0.1 published | Spec matches implementation |

---

## Epic 3: Job Manager + publishers

**Wiki:** §3.3 Job state machine  
**Docs:** [sync-runtime.md](../architecture/sync-runtime.md), ADR-001, ADR-002

| Story | Acceptance criteria |
|-------|---------------------|
| Job Manager creates `job` + enqueue | Pending → queue message |
| Per-account job mutex | Second job returns 409 |
| Retrieval publisher | InPrep → job_details + checkpoint |
| Detail queue publisher | Ready job → detail messages |
| Job consolidator | Terminal state when all details done |
| Stuck job detection | Metric + state STUCK per wiki |

---

## Epic 4: Sync Worker + directory adapters

**Wiki:** §3.5 Worker  
**Docs:** ADR-003, ADR-004, ADR-006, ADR-007

| Story | Acceptance criteria |
|-------|---------------------|
| `DirectoryPort` Azure adapter | List group members + incremental |
| Okta adapter | Groups + system log incremental |
| Google adapter | Admin SDK members |
| CREATE path | Rule match + provision + one-time assignments |
| UPDATE path | Hash compare; mapping delta only |
| DELETE path | Deprovision policy A/B/C |
| 429 retry | Batch + per-detail per wiki |
| Extensions API integration | createExtension / update / delete |

---

## Epic 5: Rules engine (configuration semantics)

**Docs:** ADR-003, [selection-criteria.md](../architecture/selection-criteria.md)

| Story | Acceptance criteria |
|-------|---------------------|
| `selection_expression` evaluator | AND criteria; priority order |
| Rule-based attribute mapping on CREATE | Site/role/group from rule |
| No update-rule evaluation | Updates ignore provision rules |

---

## Epic 6: Reporting + notifications

**PRD:** P0-9  
**Docs:** ADR-005

| Story | Acceptance criteria |
|-------|---------------------|
| Job report aggregation API | Success/fail counts + failure list |
| Service Web sync history view | Consumes report API |
| Email notifier (Phase 2) | Deferred — separate epic/story |

---

## Epic 7: Admin UI

**Docs:** [mockup-alignment.md](../ui/mockup-alignment.md), [rc-oauth-dev-setup.md](../api/rc-oauth-dev-setup.md)

| Story | Acceptance criteria |
|-------|---------------------|
| Main dashboard | Sync cards, IDP auth, sync controls, mappings, rules table, deprovision |
| Create/edit rule form | 6 sections per mockup |
| Trigger criteria builder | ALL USERS / SPECIFIC CONDITIONS AND rows |
| RC 3LO standalone dev | OAuth callback on local hostname |
| Wire to Admin API | TS client from OpenAPI |

---

## Epic 8: P1 enhancements

**PRD:** P1-1, P1-2, P1-3

| Story | Acceptance criteria |
|-------|---------------------|
| Multi-license atomic assign | After SPIKE-2 |
| Explicit no auto-purchase errors | P1-2 error codes |
| Unassigned extension priority | After SPIKE-5 |

---

## Labels / components

- `DSG`, `DSB`, `DSQ`, `directory-integration-2.0`
- Link each story to PRD ID (e.g. `P0-8`) in description

## Suggested sprint 0 (post-gate)

Epic 1 + Epic 3 skeleton + SPIKE-4 only — no RC/IDP live calls.
