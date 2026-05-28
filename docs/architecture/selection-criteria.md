# Provisioning rule selection criteria

Maps UX **Trigger Conditions** to `provisioning_assignment_rule.selection_expression` (varchar JSON, max 2048).

**Scope:** Evaluated on **CREATE (Type 1 provision)** only — not on UPDATE ([ADR-003](../adr/003-rule-triggers-and-action-sets.md)).

---

## UI modes

| Mode | UI | Stored expression |
|------|-----|-----------------|
| **ALL USERS** | Tab selected; info banner | `null` or `{ "match": "ALL" }` |
| **SPECIFIC CONDITIONS** | Criteria rows | `{ "match": "ALL", "criteria": [ ... ] }` |

Label in UI: “Matches users matching **ALL** criteria below (AND logic)”.

---

## Criteria row

| Column | Example | Field |
|--------|---------|-------|
| IDP attribute | `user.department` | `criteria[].attribute` |
| Operator | EQUALS TO (fixed Phase 1) | `criteria[].operator` = `EQ` |
| Expected value | `Sales` | `criteria[].value` |

Display on main dashboard table: `user.department == 'Sales'`.

---

## JSON schema (Phase 1)

```json
{
  "match": "ALL",
  "criteria": [
    {
      "attribute": "user.department",
      "operator": "EQ",
      "value": "Sales"
    },
    {
      "attribute": "user.group",
      "operator": "EQ",
      "value": "Engineering"
    }
  ]
}
```

---

## Evaluation algorithm

```
function ruleMatchesUser(rule, directoryUser):
  expr = parse(rule.selection_expression)
  if expr is null or expr.match == "ALL" and empty criteria:
    return true  // ALL USERS
  for c in expr.criteria:
    actual = resolvePath(directoryUser, c.attribute)
    if c.operator == "EQ" and actual != c.value:
      return false
  return true
```

**Priority:** Rules sorted by `provisioning_assignment_rule.priority` ascending. **First match wins** for applying that rule’s action set.

**No match on CREATE:** Document product default in worker (skip provision vs fallback rule).

---

## API

`ProvisioningRuleRequest.selectionExpression` in [dsg-openapi.yaml](../api/dsg-openapi.yaml).

---

## Future operators

`NE`, `IN`, `CONTAINS`, regex — deferred; UI shows only EQ in Phase 1.
