import { Link } from 'react-router-dom';
import { Plus, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
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

const TERMINAL_JOB_STATES = new Set(['COMPLETED', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'STUCK']);

function isJobFinished(state: string, completedAt: string | null): boolean {
  return TERMINAL_JOB_STATES.has(state) || completedAt != null;
}

export function DashboardPage() {
  const accountId = useAccountId();
  const [directory, setDirectory] = useState<DirectoryResponse | null>(null);
  const [latestReport, setLatestReport] = useState<JobReportResponse | null>(null);
  const [rules, setRules] = useState<ProvisioningRuleSummary[]>([]);
  const [deprovisionType, setDeprovisionType] = useState<DeprovisioningType>('FULL_DELETE');
  const [fullSyncJobId, setFullSyncJobId] = useState<string | null>(null);
  const [fullSyncStatus, setFullSyncStatus] = useState<'idle' | 'in_progress' | 'finished'>('idle');
  const [fullSyncFinishedAt, setFullSyncFinishedAt] = useState<string | null>(null);
  const [checkingFullSync, setCheckingFullSync] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fullSyncStatusRef = useRef(fullSyncStatus);
  fullSyncStatusRef.current = fullSyncStatus;

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [dir, report, ruleList, deprov, jobHistory] = await Promise.all([
        api.getDirectory(accountId).catch(() => null),
        api.getLatestReport(accountId).catch(() => null),
        api.listRules(accountId),
        api.getDeprovisioning(accountId),
        api.listJobs(accountId, 10).catch(() => ({ jobs: [] })),
      ]);
      setDirectory(dir);
      setLatestReport(report);
      setRules(ruleList.rules);
      setDeprovisionType(deprov.deprovisioningType);

      const activeFullSync = jobHistory.jobs.find(
        (job) => job.jobType === 'FULL' && !isJobFinished(job.state, job.completedAt),
      );
      if (activeFullSync) {
        setFullSyncJobId(activeFullSync.jobId);
        setFullSyncStatus('in_progress');
        setFullSyncFinishedAt(null);
      } else {
        const latestFullSync = jobHistory.jobs.find((job) => job.jobType === 'FULL');
        if (
          latestFullSync &&
          isJobFinished(latestFullSync.state, latestFullSync.completedAt)
        ) {
          setFullSyncJobId(latestFullSync.jobId);
          setFullSyncStatus('finished');
          setFullSyncFinishedAt(latestFullSync.completedAt ?? latestFullSync.startedAt);
        } else if (fullSyncStatusRef.current !== 'in_progress') {
          setFullSyncJobId(null);
          setFullSyncStatus('idle');
          setFullSyncFinishedAt(null);
        }
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load');
    }
  }, [accountId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function runFullSync() {
    setMessage(null);
    setError(null);
    try {
      const job = await api.createJob(accountId, 'FULL');
      setFullSyncJobId(job.jobId);
      setFullSyncStatus('in_progress');
      setFullSyncFinishedAt(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Full sync failed');
    }
  }

  async function checkFullSyncStatus() {
    if (!fullSyncJobId) return;
    setCheckingFullSync(true);
    setError(null);
    try {
      const report = await api.getJobReport(accountId, fullSyncJobId);
      if (isJobFinished(report.state, report.completedAt)) {
        setFullSyncStatus('finished');
        setFullSyncFinishedAt(report.completedAt ?? report.startedAt);
        setLatestReport(report);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to check sync status');
    } finally {
      setCheckingFullSync(false);
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
          <div className="flex flex-col items-end gap-1">
            <button
              type="button"
              onClick={runFullSync}
              disabled={fullSyncStatus === 'in_progress'}
              className="inline-flex items-center gap-1 rounded border border-rc-orange px-3 py-1 text-xs font-medium text-rc-orange transition-colors hover:bg-rc-orange hover:text-white disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent disabled:hover:text-rc-orange"
            >
              Full sync
            </button>
            {fullSyncStatus === 'in_progress' && (
              <div className="flex items-center gap-1 text-xs text-slate-500">
                <span>In progress</span>
                <button
                  type="button"
                  onClick={checkFullSyncStatus}
                  disabled={checkingFullSync}
                  aria-label="Check sync status"
                  className="rounded p-0.5 text-slate-500 hover:bg-slate-100 hover:text-rc-navy disabled:opacity-50"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${checkingFullSync ? 'animate-spin' : ''}`} />
                </button>
              </div>
            )}
            {fullSyncStatus === 'finished' && fullSyncFinishedAt && (
              <p className="text-xs text-slate-500">
                Finished at {formatInstant(fullSyncFinishedAt)}
              </p>
            )}
          </div>
        }
      />

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
