# ADR-007: RC / PLA provisioning port (Extensions API + stubs)

**Status:** Accepted  
**Date:** 2026-05-27  
**Related:** PLA dependency; [rc-pla-api-spec.md](../api/rc-pla-api-spec.md)

---

## Context

DSG workers must create, update, and delete RingCentral users (extensions) and apply site, role, cost center, numbers, templates, and devices. PLA is expanding provisioning APIs; only **basic extension CRUD** is available today via the public RingCentral API.

---

## Decision

### Phase 1 — real APIs (RingCentral Extensions)

Use the documented RingCentral **Extensions** REST API as the source of truth for user lifecycle:

| Operation | HTTP | Reference |
|-----------|------|-----------|
| Create | `POST /account/{accountId}/extension` | [createExtension](https://developers.ringcentral.com/api-reference/Extensions/createExtension) |
| Read | `GET /account/{accountId}/extension/{extensionId}` | [getExtension](https://developers.ringcentral.com/api-reference/Extensions/getExtension) |
| Update | `PUT /account/{accountId}/extension/{extensionId}` | [updateExtension](https://developers.ringcentral.com/api-reference/Extensions/updateExtension) |
| Delete | `DELETE /account/{accountId}/extension/{extensionId}` | [deleteExtension](https://developers.ringcentral.com/api-reference/Extensions/deleteExtension) |
| List | `GET /account/{accountId}/extension` | [listExtensions](https://developers.ringcentral.com/api-reference/Extensions/listExtensions) |

Map directory + `attribute_mapping` / `custom_attribute_mapping` into `ExtensionCreationRequest` / `ExtensionUpdateRequest`.

### Phase 1 — stubs (PLA pending)

Implement as methods on `RcProvisioningPort` with **stub adapter** until PLA documents real endpoints:

- Phone number from inventory (P0-2)
- Site, role, cost center (P0-1 rule-based attributes)
- User / call-handling templates (P0-4)
- Devices (P0-5)
- Multi-license atomic assign (P1-1)

Stub paths are prefixed `/dsg-stub/...` in [rc-pla-api-spec.md](../api/rc-pla-api-spec.md) to avoid confusion with live RC URLs.

### Provision orchestration order (Type 1)

1. `createExtension` (real)
2. Stub calls: licenses (if any), phone, site, role, cost center, devices, templates
3. Record template/device stub failures separately per ADR-003

### Local development

- `rc.client=live` — sandbox RC account + real extension API
- `rc.client=stub` — WireMock or no-op for integration tests without RC

---

## Consequences

- **Unblocks** worker and rules-engine development against a stable CRUD contract
- **PLA gate** downgraded to “stubs documented” for planning; replace stubs as PLA APIs land
- Site/role/cost center on create use stubs until PLA; rule-based mapping stored in DSB but applied via stub client

---

## References

- [gap-resolution.md](../prd/gap-resolution.md) — implementation gate
- [directory-integration-2.0.md](../prd/directory-integration-2.0.md) — dependencies
