# DSG — Directory Sync Gateway

Planning and documentation for Directory Integration 2.0 / Directory Sync Service Gateway.

**Docs:** [docs/README.md](docs/README.md)

**Planning:**

- [Wiki validation](docs/prd/wiki-validation.md)
- [Implementation gate](docs/planning/implementation-gate.md)
- [Jira epics](docs/planning/jira-epics.md)

**Local run (single backend process):**

```bash
# One-time if Maven fails with PKIX / Zscaler:
./scripts/setup-java-truststore.sh

# Start MySQL (host port 3307) + ElasticMQ, build, run API + workers:
./scripts/dev-up.sh

# Re-run backend only (stops prior process on 8080, rebuilds, starts):
./scripts/dev-run.sh

# Or manually:
docker compose up -d
mvn install -pl dsg-api -am -DskipTests
./scripts/dev-stop.sh   # if you see "Port 8080 was already in use"
mvn -pl dsg-api spring-boot:run
# Uses application-local.yml automatically when that file exists (RC server-url, client-id, etc.)
```

API base: `http://localhost:8080/dsg/v1/{accountId}/...`

Stop backend: `./scripts/dev-stop.sh`

**Admin UI (Epic 7):**

```bash
# Terminal 1: backend (see above)
# Terminal 2:
cd dsg-ui && cp .env.example .env.local && npm install && npm run dev
```

Open `http://localhost:5173/directory-integration?accountId=demo-acct` (proxies API to :8080).

RC OAuth standalone: see [docs/api/rc-oauth-dev-setup.md](docs/api/rc-oauth-dev-setup.md).

**RingCentral OAuth (local):**

```bash
cp dsg-api/src/main/resources/application-local.yml.example \
   dsg-api/src/main/resources/application-local.yml
# Edit application-local.yml — set client-id and client-secret
./scripts/dev-run.sh   # auto-activates profile `local` when file exists
```

**Build / test:**

```bash
mvn verify
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
