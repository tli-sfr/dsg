# SPIKE-4: ElasticMQ + Spring — results

**Status:** Complete  
**Date:** 2026-05-28

---

## Outcome

Validated [ADR-002](../adr/002-queue-abstraction.md): AWS SDK v2 + `MessageQueuePort` works against ElasticMQ locally and is ready for slice 1 (Job Manager).

| Task | Result |
|------|--------|
| Publish/consume `JobMessage` | Pass — `SqsMessageQueuePortIT` |
| Publish/consume `JobDetailMessage` | Pass — `SqsMessageQueuePortIT` |
| DLQ queues in config | Present in `config/elasticmq.conf` (not exercised in IT) |
| Spring profile properties | `dsg-messaging/src/main/resources/application-local.yml` |
| Optional polling listener | `JobQueuePollingListener` (`dsg.messaging.listener.enabled=true`) |

---

## Code layout

| Module | Contents |
|--------|----------|
| `dsg-domain` | `MessageQueuePort`, `JobMessage`, `JobDetailMessage` |
| `dsg-messaging` | `SqsMessageQueuePort`, auto-config, integration test |

---

## Run locally

**Maven TLS (RingCentral / Zscaler):** If you see `PKIX path building failed`, run once: `./scripts/setup-java-truststore.sh` — see [maven-tls-setup.md](../development/maven-tls-setup.md).

```bash
# Terminal 1 — queues
docker compose up -d elasticmq

# One-time on corp laptop
./scripts/setup-java-truststore.sh

# Terminal 2 — integration test (Testcontainers starts ElasticMQ)
mvn -pl dsg-messaging verify

# Or test against compose ElasticMQ:
mvn -pl dsg-messaging test -Dtest=SqsMessageQueuePortIT
```

---

## Configuration

### Local (`application-local.yml` / env)

| Property | Example |
|----------|---------|
| `dsg.messaging.endpoint` | `http://localhost:9324` |
| `dsg.messaging.region` | `us-east-1` |
| `dsg.messaging.access-key-id` | `x` (dummy for ElasticMQ) |
| `dsg.messaging.secret-access-key` | `x` |
| `dsg.messaging.job-queue-name` | `dsg-job-queue` |
| `dsg.messaging.job-detail-queue-name` | `dsg-job-detail-queue` |

### Production (no endpoint override)

| Property | Example |
|----------|---------|
| `dsg.messaging.endpoint` | *(unset)* |
| `dsg.messaging.region` | `us-west-2` |
| Credentials | IAM role / default provider chain |
| Queue names | Same as local or per-env overrides |

---

## Queue names

Aligned with [config/elasticmq.conf](../../config/elasticmq.conf):

- `dsg-job-queue` (+ `dsg-job-dlq`)
- `dsg-job-detail-queue` (+ `dsg-job-detail-dlq`)

---

## Notes

- ElasticMQ does not implement every SQS feature; run staging tests against real SQS before release.
- Production module split (`dsg-messaging-sqs`) can be a thin profile over the same `SqsMessageQueuePort` when `endpoint` is blank.
