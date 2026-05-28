# UX mockup alignment — Directory Integration v3.0

Maps Google AI Studio mockups to routes, APIs, and schema. Source: HTML export + screenshots (2026-05).

---

## Routes

| Route | Screen |
|-------|--------|
| `/directory-integration` | Main dashboard |
| `/directory-integration/rules/new` | Create rule |
| `/directory-integration/rules/{id}` | Edit rule |
| `/directory-integration/configuration` | Directory config — IDP OAuth + group selection |
| `/directory-integration/oauth/callback` | Directory IDP OAuth popup callback |

---

## §1 Main dashboard

| Section | API / data |
|---------|------------|
| Sync status cards | `GET .../jobs/latest/report` or aggregate |
| Sync History | Job list API |
| + Create Rule | Navigate to rule form |
| IDP Authorization | `PUT/GET /directory/oauth`, test |
| Synchronization | `PUT /directory`, `POST /scheduler`, `POST /jobs` |
| Attribute mappings (IDP ↔ RC) | `GET/POST /attribute-mapping`; defaults from `default_attribute_mapping` |
| Rule based automation | `POST /rule`, list rules |
| User deprovision policy | `deprovisioning_rule` API (extend admin spec) |

---

## §2 Create / edit rule (6 sections)

| # | Section | Schema |
|---|---------|--------|
| 1 | Rule & Conditions | `rule_name`, `selection_expression` — [selection-criteria.md](../architecture/selection-criteria.md) |
| 2 | Product Licenses | `license_assignment` |
| 3 | Custom Attribute Mapping (per rule) | `rule_based_attribute_mapping` (not account `custom_attribute_mapping`) |
| 4 | Phone Number Strategy | `dl_area_code_assignment` — inventory only ADR-006 |
| 5 | Device Assignment | `device_assignment` |
| 6 | Templates | `template_assignment` |

---

## §3 Trigger criteria builder

| UI | Storage |
|----|---------|
| ALL USERS | `selection_expression` null or `{ "match": "ALL" }` |
| SPECIFIC CONDITIONS + rows | AND criteria JSON |
| + Add Rule Criteria | Push to `criteria[]` |

---

## Deviations (Phase 1)

| Mockup | Implementation |
|--------|----------------|
| Rule trigger on update | **No** — Type 2 implicit mapping only |
| “Add new number automatically” | Inventory only — no purchase |
| Multi-license in descriptions | PLA stub until live API |
| V1 / V2 / V3 tabs | PM: ship v3.0 only? |

---

## Stack

React + TypeScript + Tailwind; Lucide icons; RC admin shell pattern.
