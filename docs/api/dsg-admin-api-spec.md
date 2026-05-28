# DSG Admin API (configuration)

From architectural wiki section 2.6. Service Web (SCL) → JWL → DSG.

**OpenAPI:** [dsg-openapi.yaml](dsg-openapi.yaml) v0.1  
**Runtime jobs:** [sync-runtime.md](../architecture/sync-runtime.md)

**Base path:** `/dsg/v1/{accountId}`

## Configuration endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/directory` | Configure IDP (`etm`, `directoryType`) |
| PUT | `/directory` | Set `groupId`, `active` |
| GET | `/directory` | Get directory connection |
| POST | `/scheduler` | Sync schedule (incremental/full, direction, cron) |
| POST | `/attribute-mapping` | Basic + custom mappings (Type 2 ongoing sync) |
| POST | `/rule` | Provisioning rule (priority, selection criteria, assignments) |

### Directory OAuth (Phase 1 — ETM deferred)

| Method | Path | Purpose |
|--------|------|---------|
| PUT | `/directory/oauth` | Set `client_id`, `client_secret`, provider fields |
| GET | `/directory/oauth` | Masked config + token expiry |
| POST | `/directory/oauth/test` | Validate token + directory connectivity |

Details: [directory-auth-api-spec.md](directory-auth-api-spec.md)

## Job endpoints (runtime triggers)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/jobs` | Start FULL, INCREMENTAL, or ON_DEMAND sync |
| GET | `/jobs/{jobId}/report` | Success/failure counts and failure list (P0-9 Phase 1) |

### `POST /jobs` — request

```json
{
  "jobType": "FULL",
  "externalUserIds": []
}
```

| Field | Description |
|-------|-------------|
| `jobType` | `FULL` \| `INCREMENTAL` \| `ON_DEMAND` |
| `externalUserIds` | Optional; for ON_DEMAND subset or **failed-user retry** |

### `POST /jobs` — responses

| Code | When |
|------|------|
| `202` | Job accepted; body includes `jobId`, `state` |
| `409` | Another job for this account is non-terminal (`JOB_ALREADY_RUNNING`) |

## `POST /rule` — request (summary)

```json
{
  "ruleName": "Sales Team Provisioning",
  "priority": 1,
  "selectionExpression": {
    "match": "ALL",
    "criteria": [
      { "attribute": "user.department", "operator": "EQ", "value": "Sales" }
    ]
  },
  "licenseAssignments": [
    { "licenseType": "PRIMARY_LICENSE", "licenseId": "RingEX" },
    { "licenseType": "ADD_ON_LICENSE", "licenseId": "RingCX" }
  ],
  "ruleBasedAttributeMappings": [],
  "areaCodeAssignment": { "areaCodeRuleType": "SPECIFIED_AREA_CODE", "areaCodeList": ["+1-650"] },
  "deviceAssignments": [{ "deviceType": "RingCentral App" }],
  "templateAssignments": [{ "templateType": "CALL_HANDLING", "templateId": "sales-queue-routing" }]
}
```

- `selectionExpression` omitted or `{ "match": "ALL" }` with empty criteria = **ALL USERS**
- Schema: [selection-criteria.md](../architecture/selection-criteria.md)
- Assignments map to DSB tables in [dsb-schema-wiki.md](../db/dsb-schema-wiki.md)

## RC OAuth (standalone UI)

RingCentral 3-legged OAuth for local admin UI is **not** on DSG Admin API — see [rc-oauth-dev-setup.md](rc-oauth-dev-setup.md).
