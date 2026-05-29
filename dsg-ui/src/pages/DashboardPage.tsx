import { Link } from 'react-router-dom';
import { Play, Plus, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type {
  DeprovisioningType,
  DirectoryResponse,
  JobReportResponse,
  ProvisioningRuleSummary,
} from '../api/types';
import { AttributeMappingSection } from '../components/AttributeMappingSection';
import { useAccountId } from '../components/AccountBar';
import { Card } from '../components/Card';
import { formatInstant, formatSelectionExpression } from '../lib/format';

export function DashboardPage() {
  const accountId = useAccountId();
  const [directory, setDirectory] = useState<DirectoryResponse | null>(null);
  const [latestReport, setLatestReport] = useState<JobReportResponse | null>(null);
  const [rules, setRules] = useState<ProvisioningRuleSummary[]>([]);
  const [deprovisionType, setDeprovisionType] = useState<DeprovisioningType>('FULL_DELETE');
  const [groupId, setGroupId] = useState('');
  const [cron, setCron] = useState('0 0 2 * * ?');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [dir, report, ruleList, deprov] = await Promise.all([
        api.getDirectory(accountId).catch(() => null),
        api.getLatestReport(accountId).catch(() => null),
        api.listRules(accountId),
        api.getDeprovisioning(accountId),
      ]);
      setDirectory(dir);
      setLatestReport(report);
      setRules(ruleList.rules);
      setDeprovisionType(deprov.deprovisioningType);
      if (dir?.directoryGroupId) setGroupId(dir.directoryGroupId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load');
    }
  }, [accountId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function runJob(jobType: 'FULL' | 'INCREMENTAL') {
    setMessage(null);
    setError(null);
    try {
      const job = await api.createJob(accountId, jobType);
      setMessage(`Job ${job.jobId} accepted (${job.state})`);
      setTimeout(refresh, 1500);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Job failed');
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-rc-navy">Directory Integration</h1>
          <p className="text-sm text-slate-500">Configure IDP sync, rules, and provisioning</p>
        </div>
        <button
          type="button"
          onClick={refresh}
          className="inline-flex items-center gap-2 rounded border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh
        </button>
      </div>

      {message && <p className="rounded bg-green-50 px-4 py-2 text-sm text-green-800">{message}</p>}
      {error && <p className="rounded bg-red-50 px-4 py-2 text-sm text-red-800">{error}</p>}

      <div className="grid gap-6 md:grid-cols-2">
        <Card title="Latest sync">
          {latestReport ? (
            <dl className="grid grid-cols-2 gap-2 text-sm">
              <dt className="text-slate-500">Job</dt>
              <dd>{latestReport.jobId} · {latestReport.jobType}</dd>
              <dt className="text-slate-500">State</dt>
              <dd>{latestReport.state}</dd>
              <dt className="text-slate-500">Succeeded</dt>
              <dd className="text-green-700">{latestReport.successCount}</dd>
              <dt className="text-slate-500">Failed</dt>
              <dd className={latestReport.failedCount ? 'text-red-700' : ''}>
                {latestReport.failedCount}
              </dd>
              <dt className="text-slate-500">Completed</dt>
              <dd>{formatInstant(latestReport.completedAt)}</dd>
            </dl>
          ) : (
            <p className="text-sm text-slate-500">No completed jobs yet.</p>
          )}
        </Card>

        <Card title="Directory connection">
          {directory ? (
            <dl className="space-y-1 text-sm">
              <div>
                <span className="text-slate-500">Type: </span>
                {directory.directoryType}
              </div>
              <div>
                <span className="text-slate-500">Connected: </span>
                {directory.connected ? 'Yes' : 'No'}
              </div>
            </dl>
          ) : (
            <button
              type="button"
              className="text-sm text-rc-orange underline"
              onClick={async () => {
                await api.createDirectory(accountId, 'Okta');
                refresh();
              }}
            >
              Configure Okta directory
            </button>
          )}
        </Card>
      </div>

      <AttributeMappingSection
        accountId={accountId}
        directoryType={directory?.directoryType}
        onMessage={setMessage}
        onError={setError}
      />

      <Card
        title="Synchronization"
        action={
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => runJob('INCREMENTAL')}
              className="inline-flex items-center gap-1 rounded bg-rc-orange px-3 py-1 text-xs font-medium text-white"
            >
              <Play className="h-3 w-3" />
              Incremental
            </button>
            <button
              type="button"
              onClick={() => runJob('FULL')}
              className="inline-flex items-center gap-1 rounded border border-rc-orange px-3 py-1 text-xs font-medium text-rc-orange"
            >
              Full sync
            </button>
          </div>
        }
      >
        <div className="flex flex-wrap gap-4">
          <label className="text-sm">
            Group ID
            <input
              className="ml-2 rounded border border-slate-300 px-2 py-1"
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
            />
          </label>
          <label className="text-sm">
            Cron
            <input
              className="ml-2 rounded border border-slate-300 px-2 py-1"
              value={cron}
              onChange={(e) => setCron(e.target.value)}
            />
          </label>
          <button
            type="button"
            className="self-end rounded border border-slate-300 px-3 py-1 text-sm"
            onClick={async () => {
              await api.updateDirectory(accountId, { directoryGroupId: groupId, active: true });
              await api.configureScheduler(accountId, {
                incrementalEnabled: true,
                cronExpression: cron,
                syncDirection: 'DIR_TO_RC',
              });
              setMessage('Scheduler and directory updated');
            }}
          >
            Save sync settings
          </button>
        </div>
      </Card>

      <Card
        title="Rule based automation"
        action={
          <Link
            to={`/directory-integration/rules/new?accountId=${accountId}`}
            className="inline-flex items-center gap-1 rounded bg-rc-navy px-3 py-1 text-xs font-medium text-white"
          >
            <Plus className="h-3 w-3" />
            Create rule
          </Link>
        }
      >
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b text-slate-500">
              <th className="py-2">Priority</th>
              <th>Name</th>
              <th>Conditions</th>
              <th className="py-2 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.ruleId} className="border-b border-slate-50">
                <td className="py-2">{r.priority}</td>
                <td>{r.ruleName}</td>
                <td className="text-slate-600">
                  {formatSelectionExpression(r.selectionExpression)}
                </td>
                <td className="py-2 text-right">
                  <Link
                    to={`/directory-integration/rules/${r.ruleId}?accountId=${accountId}`}
                    className="text-xs text-rc-orange hover:underline"
                  >
                    Edit
                  </Link>
                </td>
              </tr>
            ))}
            {rules.length === 0 && (
              <tr>
                <td colSpan={4} className="py-4 text-slate-500">
                  No rules configured.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>

      <Card title="User deprovision policy">
        <select
          className="rounded border border-slate-300 px-3 py-2 text-sm"
          value={deprovisionType}
          onChange={async (e) => {
            const value = e.target.value as DeprovisioningType;
            setDeprovisionType(value);
            await api.saveDeprovisioning(accountId, value);
            setMessage('Deprovision policy saved');
          }}
        >
          <option value="FULL_DELETE">Option A — Full delete</option>
          <option value="RECLAIM_RESOURCE">Option B — Reclaim resources</option>
          <option value="DISABLE_ONLY">Option C — Disable only</option>
        </select>
      </Card>
    </div>
  );
}
