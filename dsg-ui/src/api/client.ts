import type {
  ApiError,
  AttributeMappingConfig,
  DeprovisioningType,
  DirectoryResponse,
  DirectoryType,
  JobReportResponse,
  JobSummary,
  JobType,
  ProvisioningRuleSummary,
} from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/dsg/v1';

async function request<T>(
  accountId: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${API_BASE}/${accountId}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!response.ok) {
    let error: ApiError = { code: 'HTTP_ERROR', message: response.statusText };
    try {
      error = await response.json();
    } catch {
      /* ignore */
    }
    throw new Error(error.message || error.code);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const api = {
  getDirectory: (accountId: string) =>
    request<DirectoryResponse>(accountId, '/directory'),

  createDirectory: (accountId: string, directoryType: DirectoryType) =>
    request<void>(accountId, '/directory', {
      method: 'POST',
      body: JSON.stringify({ directoryType }),
    }),

  updateDirectory: (
    accountId: string,
    body: { directoryGroupId?: string; active?: boolean },
  ) =>
    request<void>(accountId, '/directory', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  configureScheduler: (
    accountId: string,
    body: { incrementalEnabled: boolean; cronExpression: string; syncDirection: string },
  ) =>
    request<void>(accountId, '/scheduler', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getAttributeMapping: (accountId: string) =>
    request<AttributeMappingConfig>(accountId, '/attribute-mapping'),

  saveAttributeMapping: (accountId: string, body: unknown) =>
    request<void>(accountId, '/attribute-mapping', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  listRules: (accountId: string) =>
    request<{ rules: ProvisioningRuleSummary[] }>(accountId, '/rules'),

  createRule: (accountId: string, body: unknown) =>
    request<void>(accountId, '/rule', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getDeprovisioning: (accountId: string) =>
    request<{ deprovisioningType: DeprovisioningType }>(accountId, '/deprovisioning'),

  saveDeprovisioning: (accountId: string, deprovisioningType: DeprovisioningType) =>
    request<void>(accountId, '/deprovisioning', {
      method: 'PUT',
      body: JSON.stringify({ deprovisioningType }),
    }),

  createJob: (accountId: string, jobType: JobType) =>
    request<{ jobId: string; state: string }>(accountId, '/jobs', {
      method: 'POST',
      body: JSON.stringify({ jobType, externalUserIds: [] }),
    }),

  getLatestReport: (accountId: string) =>
    request<JobReportResponse>(accountId, '/jobs/latest/report'),

  listJobs: (accountId: string, limit = 10) =>
    request<{ jobs: JobSummary[] }>(accountId, `/jobs?limit=${limit}`),
};
