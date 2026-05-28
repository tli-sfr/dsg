# DSG — Directory Sync Gateway

Planning and documentation for Directory Integration 2.0 / Directory Sync Service Gateway.

**Docs:** [docs/README.md](docs/README.md)

**Planning:**

- [Wiki validation](docs/prd/wiki-validation.md)
- [Implementation gate](docs/planning/implementation-gate.md)
- [Jira epics](docs/planning/jira-epics.md)

**Local run:** `docker compose up -d` (ElasticMQ + MySQL)

**Build (SPIKE-4 messaging POC):**

```bash
# One-time if Maven fails with PKIX / Zscaler:
./scripts/setup-java-truststore.sh

docker compose up -d elasticmq
mvn verify   # or: mvn -pl dsg-domain,dsg-messaging -am verify
```

Maven TLS help: [docs/development/maven-tls-setup.md](docs/development/maven-tls-setup.md)

See [docs/spikes/SPIKE-4-elasticmq-poc.md](docs/spikes/SPIKE-4-elasticmq-poc.md).

**Key decisions:**

- [Gap 1 — Trigger types (Phase 1)](docs/prd/gap-resolution.md#gap-1-three-trigger-types-p0-8-p0-6--closed-phase-1)
- [Gap 2 — Sync report (email next phase)](docs/prd/gap-resolution.md#gap-2-sync-report-notification-p0-9--closed-phase-1-partial-email-in-next-phase)
- [Gap 3 — Number assignment (inventory only)](docs/prd/gap-resolution.md#gap-3-number-assignment-p0-2--closed-phase-1--inventory-only)
- [Gap 4 — Queues (ElasticMQ / SQS)](docs/prd/gap-resolution.md#gap-4-queue-technology-elasticmq-vs-sqs--closed)
- [Gap 5 — Directory SDKs (not SCIM)](docs/prd/gap-resolution.md#gap-5-directory-integration--sdk-not-scim-p2-1--closed-phase-1)
- [PLA API — Extensions CRUD + stubs](docs/api/rc-pla-api-spec.md)
- [Gap 6 — Directory auth / OAuth in DSB (ETM deferred)](docs/prd/gap-resolution.md#gap-6-directory-authentication-etm-deferred--closed-phase-1)
