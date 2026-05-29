import { RefreshMd } from '@ringcentral/spring-icon';
import {
  Alert,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@ringcentral/spring-ui';
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
          <h1 className="typography-title text-neutral-b1">Sync History</h1>
          <p className="typography-label text-neutral-b3">View past directory synchronization jobs</p>
        </div>
        <Button variant="outlined" color="primary" size="small" startIcon={RefreshMd} onClick={refresh}>
          Refresh
        </Button>
      </div>

      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card title="Sync history">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Job</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>State</TableCell>
              <TableCell>OK</TableCell>
              <TableCell>Failed</TableCell>
              <TableCell>Started</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobs.map((j) => (
              <TableRow key={j.jobId}>
                <TableCell>
                  <span className="font-mono typography-descriptorMini">{j.jobId}</span>
                </TableCell>
                <TableCell>{j.jobType}</TableCell>
                <TableCell>{j.state}</TableCell>
                <TableCell>{j.successCount}</TableCell>
                <TableCell>{j.failedCount}</TableCell>
                <TableCell>{formatInstant(j.startedAt)}</TableCell>
              </TableRow>
            ))}
            {jobs.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <span className="typography-label text-neutral-b3">No jobs yet.</span>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}
