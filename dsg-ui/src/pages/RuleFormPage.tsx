import { Link, useNavigate, useParams } from 'react-router-dom';
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
      <div className="mx-auto max-w-3xl p-6 text-sm text-slate-500">Loading rule…</div>
    );
  }

  return (
    <form className="mx-auto max-w-3xl space-y-6 p-6" onSubmit={submit}>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-rc-navy">
          {isEdit ? 'Edit provisioning rule' : 'Create provisioning rule'}
        </h1>
        <Link
          to={`/directory-integration?accountId=${accountId}`}
          className="text-sm text-rc-orange"
        >
          Back to dashboard
        </Link>
      </div>

      {error && <p className="rounded bg-red-50 px-4 py-2 text-sm text-red-800">{error}</p>}

      <Card title="1. Rule & conditions">
        <div className="space-y-3">
          <label className="block text-sm">
            Rule name
            <input
              required
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={ruleName}
              onChange={(e) => setRuleName(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            Priority (lower = first)
            <input
              type="number"
              min={1}
              className="mt-1 w-24 rounded border border-slate-300 px-3 py-2"
              value={priority}
              onChange={(e) => setPriority(Number(e.target.value))}
            />
          </label>
          <fieldset className="text-sm">
            <legend className="font-medium">Trigger</legend>
            <label className="mt-2 flex items-center gap-2">
              <input
                type="radio"
                checked={matchAll}
                onChange={() => setMatchAll(true)}
              />
              All users
            </label>
            <label className="mt-1 flex items-center gap-2">
              <input
                type="radio"
                checked={!matchAll}
                onChange={() => setMatchAll(false)}
              />
              Specific conditions (AND)
            </label>
          </fieldset>
          {!matchAll &&
            criteria.map((c, i) => (
              <div key={i} className="flex flex-wrap gap-2">
                <input
                  className="rounded border border-slate-300 px-2 py-1 text-sm"
                  value={c.attribute}
                  onChange={(e) => {
                    const next = [...criteria];
                    next[i] = { ...c, attribute: e.target.value };
                    setCriteria(next);
                  }}
                />
                <span className="py-1 text-sm">equals</span>
                <input
                  className="rounded border border-slate-300 px-2 py-1 text-sm"
                  value={c.value}
                  onChange={(e) => {
                    const next = [...criteria];
                    next[i] = { ...c, value: e.target.value };
                    setCriteria(next);
                  }}
                />
              </div>
            ))}
          {!matchAll && (
            <button
              type="button"
              className="text-sm text-rc-orange"
              onClick={() =>
                setCriteria([
                  ...criteria,
                  { attribute: 'user.department', operator: 'EQ', value: '' },
                ])
              }
            >
              + Add criterion
            </button>
          )}
        </div>
      </Card>

      <Card title="2. Product licenses">
        <ProductLicensePicker value={licenseId} onChange={setLicenseId} />
      </Card>

      <Card title="3. Custom attribute mapping (per rule)">
        <p className="text-sm text-slate-500">Optional — configure via API for Phase 1.</p>
      </Card>

      <Card title="4. Phone number strategy">
        <label className="block text-sm">
          Area codes (comma-separated, inventory only)
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            value={areaCodes}
            onChange={(e) => setAreaCodes(e.target.value)}
          />
        </label>
      </Card>

      <Card title="5. Device assignment">
        <label className="block text-sm">
          Device type
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            value={deviceType}
            onChange={(e) => setDeviceType(e.target.value)}
          />
        </label>
      </Card>

      <Card title="6. Templates">
        <label className="block text-sm">
          Call handling template ID (optional)
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            value={templateId}
            onChange={(e) => setTemplateId(e.target.value)}
          />
        </label>
      </Card>

      <button
        type="submit"
        className="rounded bg-rc-orange px-6 py-2 text-sm font-medium text-white"
      >
        Save rule
      </button>
    </form>
  );
}
