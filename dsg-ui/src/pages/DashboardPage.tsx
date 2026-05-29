import { AddContactMd, RefreshMd } from '@ringcentral/spring-icon';
import {
  Alert,
  Button,
  IconButton,
  Link,
  MenuItemText,
  Option,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@ringcentral/spring-ui';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
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

const DEPROVISION_LABELS: Record<DeprovisioningType, string> = {
  FULL_DELETE: 'Full delete',
  RECLAIM_RESOURCE: 'Reclaim resource',
  DISABLE_ONLY: 'Disable only',
};

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
          <h1 className="typography-title text-neutral-b1">Directory Integration</h1>
          <p className="typography-label text-neutral-b3">Configure IDP sync, rules, and provisioning</p>
        </div>
        <Button variant="outlined" color="primary" size="small" startIcon={RefreshMd} onClick={refresh}>
          Refresh
        </Button>
      </div>

      {message && (
        <Alert severity="success" onClose={() => setMessage(null)}>
          {message}
        </Alert>
      )}
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <div className="grid gap-6 md:grid-cols-2">
        <Card title="Latest sync">
          {latestReport ? (
            <dl className="grid grid-cols-2 gap-2 typography-label">
              <dt className="text-neutral-b3">Job</dt>
              <dd>{latestReport.jobId} · {latestReport.jobType}</dd>
              <dt className="text-neutral-b3">State</dt>
              <dd>{latestReport.state}</dd>
              <dt className="text-neutral-b3">Succeeded</dt>
              <dd className="text-success-b">{latestReport.successCount}</dd>
              <dt className="text-neutral-b3">Failed</dt>
              <dd className={latestReport.failedCount ? 'text-danger-b' : ''}>
                {latestReport.failedCount}
              </dd>
              <dt className="text-neutral-b3">Completed</dt>
              <dd>{formatInstant(latestReport.completedAt)}</dd>
            </dl>
          ) : (
            <p className="typography-label text-neutral-b3">No completed jobs yet.</p>
          )}
        </Card>

        <Card title="Directory connection">
          {directory ? (
            <dl className="space-y-1 typography-label">
              <div>
                <span className="text-neutral-b3">Type: </span>
                {directory.directoryType}
              </div>
              <div>
                <span className="text-neutral-b3">Connected: </span>
                {directory.connected ? 'Yes' : 'No'}
              </div>
            </dl>
          ) : (
            <Button
              variant="text"
              color="primary"
              onClick={async () => {
                await api.createDirectory(accountId, 'Okta');
                refresh();
              }}
            >
              Configure Okta directory
            </Button>
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
            <Button
              variant="outlined"
              color="primary"
              size="small"
              onClick={runFullSync}
              disabled={fullSyncStatus === 'in_progress'}
            >
              Full sync
            </Button>
            {fullSyncStatus === 'in_progress' && (
              <div className="flex items-center gap-1 typography-descriptorMini text-neutral-b3">
                <span>In progress</span>
                <IconButton
                  symbol={RefreshMd}
                  size="xsmall"
                  aria-label="Check sync status"
                  disabled={checkingFullSync}
                  onClick={checkFullSyncStatus}
                />
              </div>
            )}
            {fullSyncStatus === 'finished' && fullSyncFinishedAt && (
              <p className="typography-descriptorMini text-neutral-b3">
                Finished at {formatInstant(fullSyncFinishedAt)}
              </p>
            )}
          </div>
        }
      />

      <Card
        title="Rule based automation"
        action={
          <Button
            variant="contained"
            color="secondary"
            size="small"
            startIcon={AddContactMd}
            component={RouterLink}
            to={`/directory-integration/rules/new?accountId=${accountId}`}
          >
            Create rule
          </Button>
        }
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Priority</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Conditions</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rules.map((r) => (
              <TableRow key={r.ruleId}>
                <TableCell>{r.priority}</TableCell>
                <TableCell>{r.ruleName}</TableCell>
                <TableCell>{formatSelectionExpression(r.selectionExpression)}</TableCell>
                <TableCell align="right">
                  <Link
                    component={RouterLink}
                    to={`/directory-integration/rules/${r.ruleId}?accountId=${accountId}`}
                  >
                    Edit
                  </Link>
                </TableCell>
              </TableRow>
            ))}
            {rules.length === 0 && (
              <TableRow>
                <TableCell colSpan={4}>
                  <span className="typography-label text-neutral-b3">No rules configured.</span>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>

      <Card title="User deprovision policy">
        <Select
          value={deprovisionType}
          className="w-56"
          renderValue={(value) => DEPROVISION_LABELS[value as DeprovisioningType]}
          MenuProps={{ className: 'min-w-[14rem]' }}
          onChange={async (e) => {
            const value = e.target.value as DeprovisioningType;
            setDeprovisionType(value);
            await api.saveDeprovisioning(accountId, value);
            setMessage('Deprovision policy saved');
          }}
        >
          <Option value="FULL_DELETE">
            <MenuItemText>{DEPROVISION_LABELS.FULL_DELETE}</MenuItemText>
          </Option>
          <Option value="RECLAIM_RESOURCE">
            <MenuItemText>{DEPROVISION_LABELS.RECLAIM_RESOURCE}</MenuItemText>
          </Option>
          <Option value="DISABLE_ONLY">
            <MenuItemText>{DEPROVISION_LABELS.DISABLE_ONLY}</MenuItemText>
          </Option>
        </Select>
      </Card>
    </div>
  );
}
