# ADR-002: Queue abstraction (DSQ — ElasticMQ / SQS)

**Status:** Accepted  
**Date:** 2026-05-27  
**Related:** [gap-resolution.md](../prd/gap-resolution.md#gap-4-queue-technology-elasticmq-vs-sqs--closed)

---

## Context

The DSG wiki defines **DSQ** as AWS SQS (job queue + job-detail queue). The team uses **Java + Spring Boot** workers and wants local development **without AWS credentials**.

ElasticMQ implements an **SQS-compatible HTTP API**. The same client configuration and message contracts work against ElasticMQ locally and AWS SQS in production.

---

## Decision

### Compatibility principle

| Property | ElasticMQ (local) | AWS SQS (production) |
|----------|-------------------|----------------------|
| Client API | SQS-compatible (AWS SDK v2 endpoint override) | Native SQS |
| Message format | Same JSON job / job-detail payloads | Same |
| Application code | `MessageQueuePort` only | `MessageQueuePort` only |

**Local-first preference:** Developers run **ElasticMQ first** for all queue development and integration tests. Production deploys point the same adapter at real SQS endpoints.

### Environment mapping

| Environment | Queue | How to run |
|-------------|--------|------------|
| **Local / CI (default)** | **ElasticMQ** | `docker compose up -d` — [docker-compose.yml](../../docker-compose.yml) |
| Production | AWS SQS | `MessageQueuePort` with AWS endpoint + IAM |

### `MessageQueuePort` (domain layer)

- `publishJob(JobMessage)`
- `publishJobDetail(JobDetailMessage)`
- `receiveJob()` / `receiveJobDetail()` — consumer side

Implementation modules:

- `dsg-messaging-elasticmq` — local/CI (endpoint `http://localhost:9324`)
- `dsg-messaging-sqs` — production

Spring profile example: `local` → ElasticMQ adapter; `prod` → SQS adapter.

### Queue names (wiki-aligned)

| Queue | Purpose |
|-------|---------|
| `dsg-job-queue` | Account-level job; batch 429 retry |
| `dsg-job-detail-queue` | Per-user sync work |

Pre-created in [config/elasticmq.conf](../../config/elasticmq.conf). DLQs: `dsg-job-dlq`, `dsg-job-detail-dlq`.

**SPIKE-4:** Implementation in `dsg-messaging` — [SPIKE-4-elasticmq-poc.md](../spikes/SPIKE-4-elasticmq-poc.md).

### Local developer setup

```bash
docker compose up -d elasticmq mysql
```

Spring / AWS SDK local settings (example):

```properties
aws.region=us-east-1
aws.accessKeyId=x
aws.secretAccessKey=x
spring.cloud.aws.sqs.endpoint=http://localhost:9324
```

Use dummy credentials with ElasticMQ; never commit real AWS keys for local runs.

---

## Consequences

### Positive

- **ElasticMQ first** — fast local loop, no AWS account required
- SQS compatibility reduces environment-specific bugs
- Wiki DSQ design unchanged semantically (two queues, same flows)

### Negative

- ElasticMQ does not replicate every SQS feature (e.g. FIFO nuances); validate in staging against real SQS before release
- Team must run Docker for local queues (documented in compose file)

---

## References

- [dsg-design-wiki.md](../architecture/dsg-design-wiki.md) — section 2.5 DSQ
- [overview.md](../architecture/overview.md)
