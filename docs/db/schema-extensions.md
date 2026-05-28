# DSB schema extensions (beyond wiki)

PRD/gap-driven additions not in the original Confluence ER diagrams.

| Extension | Document | Reason |
|-----------|----------|--------|
| `account_directory_oauth` | [schema-auth-extensions.md](schema-auth-extensions.md) | ETM not ready; AES-256 OAuth credentials in DSB |
| `default_attribute_mapping` | [dsb-schema-wiki.md](dsb-schema-wiki.md) §1.8 | Per–directory-type default IDP→RC rows for Admin UI |

**Wiki baseline (29 tables):** [dsb-schema-wiki.md](dsb-schema-wiki.md) — plus `default_attribute_mapping` and `account_directory_oauth` in Flyway (**31** application tables).

No other `schema-extensions` tables are required for Phase 1 gap closure. Future candidates:

- `directory_writeback_log` (P0-3 idempotency — not yet designed)
- Notification recipient config (P0-9 email — next phase)
