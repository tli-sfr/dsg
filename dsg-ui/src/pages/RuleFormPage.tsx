import { AddContactMd } from '@ringcentral/spring-icon';
import {
  Alert,
  Button,
  FormLabel,
  Link,
  Radio,
  RadioGroup,
  TextField,
} from '@ringcentral/spring-ui';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { SelectionCriterion } from '../api/types';
import { useAccountId } from '../components/AccountBar';
import { Card } from '../components/Card';
import { ProductLicensePicker } from '../components/ProductLicensePicker';
import type { ProductLicenseId } from '../lib/productLicenses';

function isProductLicenseId(value: string): value is ProductLicenseId {
  return value === 'VideoPro' || value === 'VideoProPlus' || value === 'RingEX';
}

export function RuleFormPage() {
  const { ruleId: ruleIdParam } = useParams();
  const isEdit = Boolean(ruleIdParam);
  const accountId = useAccountId();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(isEdit);
  const [ruleName, setRuleName] = useState('');
  const [priority, setPriority] = useState(1);
  const [matchAll, setMatchAll] = useState(true);
  const [criteria, setCriteria] = useState<SelectionCriterion[]>([
    { attribute: 'user.department', operator: 'EQ', value: 'Sales' },
  ]);
  const [licenseId, setLicenseId] = useState<ProductLicenseId>('RingEX');
  const [areaCodes, setAreaCodes] = useState('+1-650');
  const [deviceType, setDeviceType] = useState('RingCentral App');
  const [templateId, setTemplateId] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit || !ruleIdParam) {
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const rule = await api.getRule(accountId, ruleIdParam);
        if (cancelled) {
          return;
        }
        setRuleName(rule.ruleName);
        setPriority(rule.priority);
        const expr = rule.selectionExpression;
        const allUsers = expr?.match === 'ALL' && !Array.isArray(expr.criteria);
        setMatchAll(allUsers);
        if (!allUsers && Array.isArray(expr?.criteria)) {
          setCriteria(
            (expr.criteria as SelectionCriterion[]).map((c) => ({
              attribute: String(c.attribute ?? ''),
              operator: String(c.operator ?? 'EQ'),
              value: String(c.value ?? ''),
            })),
          );
        }
        const primary = rule.licenseAssignments?.[0]?.licenseId;
        if (primary && isProductLicenseId(primary)) {
          setLicenseId(primary);
        }
        const codes = rule.areaCodeAssignment?.areaCodeList;
        if (codes?.length) {
          setAreaCodes(codes.join(', '));
        }
        const device = rule.deviceAssignments?.[0]?.deviceType;
        if (device) {
          setDeviceType(device);
        }
        const template = rule.templateAssignments?.[0]?.templateId;
        if (template) {
          setTemplateId(template);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load rule');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [accountId, isEdit, ruleIdParam]);

  function buildPayload() {
    const selectionExpression = matchAll
      ? { match: 'ALL' }
      : { match: 'ALL', criteria };

    return {
      ruleName,
      priority,
      selectionExpression,
      licenseAssignments: [{ licenseType: 'PRIMARY_LICENSE', licenseId }],
      areaCodeAssignment: {
        areaCodeRuleType: 'SPECIFIED_AREA_CODE',
        areaCodeList: areaCodes.split(',').map((s) => s.trim()).filter(Boolean),
      },
      deviceAssignments: [{ deviceType }],
      templateAssignments: templateId
        ? [{ templateType: 'CALL_HANDLING', templateId }]
        : [],
      ruleBasedAttributeMappings: [],
    };
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const payload = buildPayload();

    try {
      if (isEdit && ruleIdParam) {
        await api.updateRule(accountId, ruleIdParam, payload);
      } else {
        await api.createRule(accountId, payload);
      }
      navigate(`/directory-integration?accountId=${accountId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl p-6 typography-label text-neutral-b3">Loading rule…</div>
    );
  }

  return (
    <form className="mx-auto max-w-3xl space-y-6 p-6" onSubmit={submit}>
      <div className="flex items-center justify-between">
        <h1 className="typography-title text-neutral-b1">
          {isEdit ? 'Edit provisioning rule' : 'Create provisioning rule'}
        </h1>
        <Link component={RouterLink} to={`/directory-integration?accountId=${accountId}`}>
          Back to dashboard
        </Link>
      </div>

      {error && <Alert severity="error">{error}</Alert>}

      <Card title="1. Rule & conditions">
        <div className="space-y-4">
          <div className="flex items-end gap-4">
            <TextField
              fullWidth
              label="Rule name"
              required
              value={ruleName}
              onChange={(e) => setRuleName(e.target.value)}
              RootProps={{ className: 'min-w-0 flex-1' }}
            />
            <TextField
              fullWidth
              label="Priority (lower = first)"
              type="number"
              value={String(priority)}
              onChange={(e) => setPriority(Number(e.target.value))}
              RootProps={{ className: 'w-28 shrink-0' }}
            />
          </div>
          <fieldset>
            <FormLabel className="typography-descriptorMini uppercase text-neutral-b3">
              Trigger
            </FormLabel>
            <RadioGroup
              name="trigger"
              value={matchAll ? 'all' : 'conditions'}
              onChange={(e) => setMatchAll(e.target.value === 'all')}
            >
              <label className="mt-2 flex items-center gap-2 typography-label uppercase">
                <Radio value="all" />
                All users
              </label>
              <label className="mt-1 flex items-center gap-2 typography-label uppercase">
                <Radio value="conditions" />
                Specific conditions (AND)
              </label>
            </RadioGroup>
          </fieldset>
          {!matchAll &&
            criteria.map((c, i) => (
              <div key={i} className="flex items-center gap-3">
                <TextField
                  fullWidth
                  value={c.attribute}
                  onChange={(e) => {
                    const next = [...criteria];
                    next[i] = { ...c, attribute: e.target.value };
                    setCriteria(next);
                  }}
                  RootProps={{ className: 'min-w-0 flex-1' }}
                />
                <span className="shrink-0 typography-descriptorMini uppercase text-neutral-b3">
                  Equals
                </span>
                <TextField
                  fullWidth
                  value={c.value}
                  onChange={(e) => {
                    const next = [...criteria];
                    next[i] = { ...c, value: e.target.value };
                    setCriteria(next);
                  }}
                  RootProps={{ className: 'min-w-0 flex-1' }}
                />
              </div>
            ))}
          {!matchAll && (
            <Button
              type="button"
              variant="text"
              color="primary"
              size="small"
              startIcon={AddContactMd}
              onClick={() =>
                setCriteria([
                  ...criteria,
                  { attribute: 'user.department', operator: 'EQ', value: '' },
                ])
              }
            >
              Add criterion
            </Button>
          )}
        </div>
      </Card>

      <Card title="2. Product licenses">
        <ProductLicensePicker value={licenseId} onChange={setLicenseId} />
      </Card>

      <Card title="3. Custom attribute mapping (per rule)">
        <p className="typography-label text-neutral-b3">Optional — configure via API for Phase 1.</p>
      </Card>

      <Card title="4. Phone number strategy">
        <TextField
          label="Area codes (comma-separated, inventory only)"
          value={areaCodes}
          onChange={(e) => setAreaCodes(e.target.value)}
        />
      </Card>

      <Card title="5. Device assignment">
        <TextField
          label="Device type"
          value={deviceType}
          onChange={(e) => setDeviceType(e.target.value)}
        />
      </Card>

      <Card title="6. Templates">
        <TextField
          label="Call handling template ID (optional)"
          value={templateId}
          onChange={(e) => setTemplateId(e.target.value)}
        />
      </Card>

      <Button type="submit" variant="contained" color="primary">
        Save rule
      </Button>
    </form>
  );
}
