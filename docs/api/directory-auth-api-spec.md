# Directory authentication API (DSG Admin)

Configures account-level OAuth credentials when **ETM is not used** (`dsg.auth=dsb-oauth`).

**Port:** [ADR-008](../adr/008-directory-auth-port.md)  
**Schema:** [schema-auth-extensions.md](../db/schema-auth-extensions.md)

---

## Configure OAuth credentials

```http
PUT /dsg/v1/{accountId}/directory/oauth
Content-Type: application/json
```

### Azure example

```json
{
  "directoryType": "Azure",
  "authFlow": "CLIENT_CREDENTIALS",
  "clientId": "00000000-0000-0000-0000-000000000000",
  "clientSecret": "********",
  "azureTenantId": "00000000-0000-0000-0000-000000000000",
  "scopes": "https://graph.microsoft.com/.default"
}
```

### Okta example (OAuth client credentials)

```json
{
  "directoryType": "Okta",
  "authFlow": "CLIENT_CREDENTIALS",
  "clientId": "0oa...",
  "clientSecret": "********",
  "oktaDomain": "https://dev-example.okta.com",
  "scopes": "okta.groups.read okta.users.read"
}
```

### Google example (service account)

```json
{
  "directoryType": "Google",
  "authFlow": "GOOGLE_SERVICE_ACCOUNT",
  "googleWorkspaceAdmin": "admin@customer.com",
  "googleServiceAccountKeyJson": "{ ... }",
  "scopes": "https://www.googleapis.com/auth/admin.directory.user.readonly"
}
```

**Response:** `200` — `{ "oauthConfigId": 42, "directoryType": "Azure" }` (no secrets).

**Persistence:** `clientSecret` encrypted with **AES-256** before insert (`client_secret_enc`); encryption key from `dsg.crypto.secret-key` — see [ADR-008](../adr/008-directory-auth-port.md).

---

## Get OAuth config (masked)

```http
GET /dsg/v1/{accountId}/directory/oauth
```

```json
{
  "directoryType": "Azure",
  "authFlow": "CLIENT_CREDENTIALS",
  "clientId": "00000000-0000-0000-0000-000000000000",
  "azureTenantId": "00000000-0000-0000-0000-000000000000",
  "accessTokenExpiresAt": "2026-05-27T20:00:00Z",
  "hasClientSecret": true,
  "hasRefreshToken": false
}
```

---

## Test connection (optional)

```http
POST /dsg/v1/{accountId}/directory/oauth/test
```

Calls `DirectoryAuthPort.getAccessToken` and a lightweight directory API (e.g. list groups). Returns success or error for Service Web UI.

---

## Wiki `POST /directory` with ETM (future)

When `dsg.auth=etm`:

```json
{
  "etm": "exkbez2ie3cPfSkhP0h7",
  "directoryType": "Azure"
}
```

No `client_secret` in DSG request — ETM owns credentials.
