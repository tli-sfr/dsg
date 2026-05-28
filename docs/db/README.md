# DSB (Directory Sync Database) — schema documentation

The wiki defines the full MySQL schema for DSG. Local docs are split as follows:

| Document | Contents |
|----------|----------|
| [dsb-schema-wiki.md](dsb-schema-wiki.md) | **All wiki tables** (sections 2.4.1–2.4.2) — translated from Confluence |
| [schema-auth-extensions.md](schema-auth-extensions.md) | **Phase 1 extension** — `account_directory_oauth` (ETM deferred) + AES-256 |
| [erd.md](erd.md) | Mermaid ER diagram (wiki + oauth extension) |
| [erd-from-wiki.mmd](erd-from-wiki.mmd) | Mermaid ER source for diagram tooling |

**Why it looked like “one table”:** Early gap work only added the **new** table not in the wiki (`account_directory_oauth`). Wiki baseline tables were summarized in [dsg-design-wiki.md](../architecture/dsg-design-wiki.md) section 4 but not yet copied into `docs/db/` until now.

**Implementation:** Flyway migrations should implement `dsb-schema-wiki.md` + `schema-auth-extensions.md`.
