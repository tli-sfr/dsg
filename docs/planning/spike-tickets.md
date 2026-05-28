# Spike tickets — Directory Integration 2.0 / DSG

Planning spikes to unblock implementation. Create corresponding Jira tickets under the DSG program.

---

## SPIKE-1: IDP write-back feasibility (P0-3)

| Field | Value |
|-------|--------|
| **Owner** | Backend |
| **Duration** | 3–5 days |
| **Unblocks** | ADR-010, reverse sync worker |

**Goal:** Prove RC → IDP PATCH/PUT for mapped fields on Okta, Azure AD, and Google Workspace.

**Tasks:**

1. List writable attributes per provider API vs `attribute_mapping` RC→DIR rows.
2. Document rate limits and 409 conflict behavior.
3. Prototype idempotent retry using hash skip + provider error codes.
4. Recommend `idempotency_key` storage shape.

**Done when:** Written spike report linked from ADR-010; go/no-go per IDP.

---

## SPIKE-2: PLA multi-license atomic API (P1-1)

| Field | Value |
|-------|--------|
| **Owner** | PLA + Backend |
| **Duration** | 2–3 days |
| **Unblocks** | Live `RcProvisioningPort.assignLicensesAtomic` |

**Goal:** Confirm whether RC/PLA exposes atomic multi-license assign or requires compensating rollback.

**Tasks:**

1. Identify production API for RingEX + RingCX combo from rule mockup.
2. Define stub → live migration in [rc-pla-api-spec.md](../api/rc-pla-api-spec.md).
3. Document failure partial-assign behavior.

**Done when:** API contract signed by PLA; stub interface frozen.

---

## SPIKE-3: License combination matrix (P1-1 UI)

| Field | Value |
|-------|--------|
| **Owner** | PM + Frontend |
| **Duration** | 1–2 days |
| **Unblocks** | Rule form license picker validation |

**Goal:** Authoritative matrix of allowed PRIMARY + ADD_ON combinations per account SKU.

**Done when:** `docs/prd/license-matrix.md` published and linked from UI spec.

---

## SPIKE-4: ElasticMQ + Spring listener POC (Gap 4) — **Complete**

| Field | Value |
|-------|--------|
| **Owner** | Backend |
| **Duration** | 2 days |
| **Unblocks** | Job Manager + publisher skeleton |
| **Results** | [SPIKE-4-elasticmq-poc.md](../spikes/SPIKE-4-elasticmq-poc.md) |

**Goal:** Verify `MessageQueuePort` with AWS SDK v2 endpoint override against `docker compose` ElasticMQ.

**Delivered:**

1. `dsg-domain` — `MessageQueuePort`, `JobMessage`, `JobDetailMessage`
2. `dsg-messaging` — `SqsMessageQueuePort`, Spring auto-config, `SqsMessageQueuePortIT` (Testcontainers)
3. `application-local.yml` + [SPIKE-4 doc](../spikes/SPIKE-4-elasticmq-poc.md)
4. Optional `JobQueuePollingListener` for scheduled polling POC

**Verify locally:** `docker compose up -d elasticmq` then `mvn -pl dsg-messaging verify`

---

## SPIKE-5: P1-3 Unassigned extension consumption

| Field | Value |
|-------|--------|
| **Owner** | PLA + Backend |
| **Duration** | 2 days |
| **Unblocks** | P1-3 traceability row |

**Goal:** Locate PLA/API for preferring unassigned extensions before create.

**Done when:** API documented or explicitly deferred with PM sign-off.

---

## Priority order

1. SPIKE-4 (local pipeline proof)
2. SPIKE-2 + SPIKE-3 (license UX)
3. SPIKE-1 (reverse sync)
4. SPIKE-5 (P1)
