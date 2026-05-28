# DSG API specifications

## RC / PLA provisioning (DSG Worker → RingCentral)

**[rc-pla-api-spec.md](rc-pla-api-spec.md)** — primary contract.

| Category | Phase 1 |
|----------|---------|
| Extension create / update / delete / get / list | [RingCentral Extensions API](https://developers.ringcentral.com/api-reference/Extensions/createExtension) |
| Site, role, cost center, phone, templates, devices, licenses | Stub (`/dsg-stub/...`) — see spec |

**ADR:** [007-rc-pla-provisioning-port.md](../adr/007-rc-pla-provisioning-port.md)

## DSG Admin API (Service Web → DSG)

**[dsg-admin-api-spec.md](dsg-admin-api-spec.md)** — configuration APIs from wiki section 2.6.

**[dsg-openapi.yaml](dsg-openapi.yaml)** — OpenAPI 3.0 v0.1 (machine-readable).

**[directory-auth-api-spec.md](directory-auth-api-spec.md)** — IDP OAuth when ETM deferred.

**[rc-oauth-dev-setup.md](rc-oauth-dev-setup.md)** — RingCentral 3-legged OAuth for standalone local UI.
