# RC / PLA provisioning API specification (Phase 1)

Contract for DSG sync workers calling RingCentral. **PLA dependency** — see [ADR-007](../adr/007-rc-pla-provisioning-port.md).

## Status summary

| Operation | Phase 1 | API source |
|-----------|---------|------------|
| Create extension | **Real** | [Create Extension](https://developers.ringcentral.com/api-reference/Extensions/createExtension) |
| Get extension | **Real** | [Get Extension](https://developers.ringcentral.com/api-reference/Extensions/getExtension) |
| Update extension | **Real** | [Update Extension](https://developers.ringcentral.com/api-reference/Extensions/updateExtension) |
| List extensions | **Real** | [List Extensions](https://developers.ringcentral.com/api-reference/Extensions/listExtensions) |
| Delete extension | **Real** | [Delete Extension](https://developers.ringcentral.com/api-reference/Extensions/deleteExtension) |
| Assign phone (inventory) | **Stub** | PLA TBD — [ADR-006](../adr/006-number-assignment-inventory-only.md) |
| Assign site | **Stub** | PLA TBD |
| Assign role | **Stub** | PLA TBD |
| Assign cost center | **Stub** | PLA TBD |
| Apply templates | **Stub** | PLA TBD — [ADR-003](../adr/003-rule-triggers-and-action-sets.md) |
| Assign devices | **Stub** | PLA TBD |
| Multi-license assign | **Stub (P1)** | PLA TBD |

**Base URL:** `https://platform.ringcentral.com/restapi/v1.0`

---

## Implemented — RingCentral Extensions API

### Create extension (Type 1 provision)

```http
POST /account/{accountId}/extension
Content-Type: application/json
```

**Request body** (subset of RingCentral `ExtensionCreationRequest`):

```json
{
  "contact": {
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "department": "Engineering"
  },
  "type": "User",
  "status": "Enabled"
}
```

**Response:** `200` + `ExtensionResource` (`id`, `extensionNumber`, `contact`, `status`, `uri`).

**DSG mapping:** Directory user + `attribute_mapping` / `custom_attribute_mapping` → `contact` and extension fields.

---

### Update extension (Type 2 implicit mapping sync)

```http
PUT /account/{accountId}/extension/{extensionId}
Content-Type: application/json
```

**Request body** (subset of `ExtensionUpdateRequest`):

```json
{
  "contact": {
    "department": "Sales",
    "jobTitle": "Manager"
  },
  "status": "Enabled"
}
```

**DSG mapping:** Only fields that changed per hash comparison on mapped directory attributes.

---

### Get extension

```http
GET /account/{accountId}/extension/{extensionId}
```

Used for reconciliation, retry, and RC → directory write-back source data.

---

### List extensions

```http
GET /account/{accountId}/extension?page=1&perPage=100
```

Used for RC → directory full sync (wiki 2.3.5).

---

### Delete extension (deprovision — partial)

```http
DELETE /account/{accountId}/extension/{extensionId}
```

**Note:** PRD options A/B/C may require additional stub APIs (reclaim number/license, disable-only). Deprovision orchestration in worker calls extension delete and/or stubs per `deprovisioning_type`.

---

## Stubs — PLA placeholders (`/dsg-stub/...`)

Paths are **not** real RingCentral URLs. They define the `RcProvisioningPort` methods until PLA ships APIs. Local profile uses `RcProvisioningStubClient` (no-op or WireMock).

### Assign phone number from inventory

```http
POST /dsg-stub/account/{accountId}/extension/{extensionId}/phone-number
```

```json
{
  "countryCode": 1,
  "areaCode": "650",
  "preferInventory": true
}
```

**Stub response:**

```json
{ "accepted": true, "stub": true, "message": "PLA API not wired" }
```

**Errors (when implemented):** `INSUFFICIENT_NUMBER_INVENTORY` → `job_detail.comment`.

---

### Assign site

```http
PUT /dsg-stub/account/{accountId}/extension/{extensionId}/site
```

```json
{ "siteId": "12345" }
```

---

### Assign role

```http
PUT /dsg-stub/account/{accountId}/extension/{extensionId}/role
```

```json
{ "roleId": "67890" }
```

---

### Assign cost center

```http
PUT /dsg-stub/account/{accountId}/extension/{extensionId}/cost-center
```

```json
{ "costCenterCode": "CC-100" }
```

---

### Apply templates (P0-4)

```http
POST /dsg-stub/account/{accountId}/extension/{extensionId}/templates
```

```json
{
  "userTemplateId": "34132152341",
  "callHandlingTemplateId": "6143510954"
}
```

Worker: failure here records `job_detail` / separate log; **does not** fail create.

---

### Assign devices (P0-5)

```http
POST /dsg-stub/account/{accountId}/extension/{extensionId}/devices
```

```json
{
  "devices": [
    { "deviceType": "Softphone" },
    { "deviceType": "InventoryPhone", "deviceId": "device-abc" }
  ]
}
```

---

### Multi-license assign (P1 — stub)

```http
PUT /dsg-stub/account/{accountId}/extension/{extensionId}/licenses
```

```json
{
  "licenseIds": ["RingEX", "RingCX"]
}
```

---

## Java port (`RcProvisioningPort`)

```java
public interface RcProvisioningPort {
  ExtensionResource createExtension(String accountId, ExtensionCreationRequest request);
  ExtensionResource updateExtension(String accountId, String extensionId, ExtensionUpdateRequest request);
  ExtensionResource getExtension(String accountId, String extensionId);
  void deleteExtension(String accountId, String extensionId);

  // Stubs — default no-op in RcProvisioningStubClient
  void assignPhoneFromInventory(String accountId, String extensionId, PhoneAssignRequest request);
  void assignSite(String accountId, String extensionId, String siteId);
  void assignRole(String accountId, String extensionId, String roleId);
  void assignCostCenter(String accountId, String extensionId, String costCenterCode);
  void applyTemplates(String accountId, String extensionId, TemplateApplyRequest request);
  void assignDevices(String accountId, String extensionId, DeviceAssignRequest request);
  void assignLicensesAtomic(String accountId, String extensionId, List<String> licenseIds);
}
```

**Implementations:**

| Bean | Profile | Behavior |
|------|---------|----------|
| `RcExtensionClient` | `rc.client=live` | RingCentral REST SDK / HTTP for extension CRUD |
| `RcProvisioningStubClient` | `rc.client=stub` | CRUD optional WireMock; stub methods log + return |

---

## OpenAPI machine-readable export

When out of planning mode, export this spec to `rc-pla-openapi.yaml` from this document for code generation.
