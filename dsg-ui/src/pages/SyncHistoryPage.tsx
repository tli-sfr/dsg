import { RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { JobSummary } from '../api/types';
import { useAccountId } from '../components/AccountBar';
import { Card } from '../components/Card';
import { formatInstant } from '../lib/format';

export function SyncHistoryPage() {
  const accountId = useAccountId();
  const [jobs, setJobs] = useState<JobSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const history = await api.listJobs(accountId, 10);
      setJobs(history.jobs);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load sync history');
    }
  }, [accountId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-rc-navy">Sync History</h1>
          <p className="text-sm text-slate-500">View past directory synchronization jobs</p>
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

      {error && <p className="rounded bg-red-50 px-4 py-2 text-sm text-red-800">{error}</p>}

      <Card title="Sync history">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b text-slate-500">
              <th className="py-2">Job</th>
              <th>Type</th>
              <th>State</th>
              <th>OK</th>
              <th>Failed</th>
              <th>Started</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((j) => (
              <tr key={j.jobId} className="border-b border-slate-50">
                <td className="py-2 font-mono text-xs">{j.jobId}</td>
                <td>{j.jobType}</td>
                <td>{j.state}</td>
                <td>{j.successCount}</td>
                <td>{j.failedCount}</td>
                <td>{formatInstant(j.startedAt)}</td>
              </tr>
            ))}
            {jobs.length === 0 && (
              <tr>
                <td colSpan={6} className="py-4 text-slate-500">
                  No jobs yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
