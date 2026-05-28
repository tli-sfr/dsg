# DSB entity relationship diagram

Mermaid source (wiki baseline + `account_directory_oauth`). Copy to `erd.mmd` if your tooling requires `.mmd`.

```mermaid
erDiagram
  directory_type ||--o{ account_directory_auth : has
  directory_type ||--o{ account_directory_oauth : has
  directory_type ||--o{ directory_attribute : defines

  account_directory_auth |o--o| account_directory_oauth : oauth_config_id
  account_directory_auth ||--o{ directory_sync_checkpoint : tracks

  provisioning_assignment_rule ||--o{ license_assignment : has
  provisioning_assignment_rule ||--o{ template_assignment : has
  provisioning_assignment_rule ||--o{ device_assignment : has
  provisioning_assignment_rule ||--o{ dl_area_code_assignment : has
  provisioning_assignment_rule ||--o{ rule_based_attribute_mapping : has

  job ||--o{ job_detail : contains
  job_type ||--o{ job : classifies
  job_state ||--o{ job : status
  job_state ||--o{ job_detail : status
  operation_type ||--o{ job_detail : operation
```

Full table definitions: [dsb-schema-wiki.md](dsb-schema-wiki.md).
