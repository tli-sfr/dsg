import type {
  ApiError,
  AttributeMappingConfig,
  DeprovisioningType,
  DirectoryResponse,
  DirectoryType,
  JobReportResponse,
  JobSummary,
  JobType,
  ProvisioningRuleDetail,
  ProvisioningRuleSummary,
} from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/dsg/v1';

async function request<T>(
  accountId: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${API_BASE}/${accountId}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!response.ok) {
    const fallback = `Request failed (${response.status})`;
    let message = response.statusText || fallback;
    try {
      const body = (await response.json()) as ApiError & {
        error?: string;
        path?: string;
      };
      message = body.message || body.error || body.code || fallback;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(message);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export const api = {
  getDirectory: (accountId: string) =>
    request<DirectoryResponse>(accountId, '/directory'),

  updateDirectory: (
    accountId: string,
    body: { directoryGroupId?: string; directoryGroupName?: string; active?: boolean },
  ) =>
    request<void>(accountId, '/directory', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  getDirectoryOAuthConfig: (accountId: string) =>
    request<import('./types').DirectoryOAuthConfig>(accountId, '/directory/oauth/config'),

  saveDirectoryOAuth: (
    accountId: string,
    body: {
      directoryType: DirectoryType;
      authFlow: string;
      clientId: string;
      clientSecret: string;
      azureTenantId?: string;
      oktaDomain?: string;
    },
  ) =>
    request<void>(accountId, '/directory/oauth', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  getDirectoryAuthorizeUrl: (accountId: string) =>
    request<{ authorizeUrl: string; state: string }>(accountId, '/directory/oauth/authorize-url'),

  exchangeDirectoryOAuthToken: (
    accountId: string,
    body: { code: string; state: string },
  ) =>
    request<{
      status: string;
      connectedUserFirstName: string | null;
      connectedUserLastName: string | null;
    }>(accountId, '/directory/oauth/token', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  disconnectDirectoryOAuth: (accountId: string) =>
    request<void>(accountId, '/directory/oauth', { method: 'DELETE' }),

  searchDirectoryGroups: (accountId: string, search: string) =>
    request<{ groups: { id: string; name: string }[] }>(
      accountId,
      `/directory/groups?search=${encodeURIComponent(search)}`,
    ),

  createDirectory: (accountId: string, directoryType: DirectoryType) =>
    request<void>(accountId, '/directory', {
      method: 'POST',
      body: JSON.stringify({ directoryType }),
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

  getRule: (accountId: string, ruleId: string) =>
    request<ProvisioningRuleDetail>(accountId, `/rules/${ruleId}`),

  createRule: (accountId: string, body: unknown) =>
    request<void>(accountId, '/rule', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  updateRule: (accountId: string, ruleId: string, body: unknown) =>
    request<void>(accountId, `/rule/${ruleId}`, {
      method: 'PUT',
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

  getRcOAuthStatus: (accountId: string) =>
    request<{ configured: boolean; connected: boolean }>(accountId, '/rc/oauth/status'),

  getRcOAuthSession: (accountId: string) =>
    request<{
      rcAccountId: string;
      extensionId: number;
      extensionNumber: string;
      extensionName: string | null;
    }>(accountId, '/rc/oauth/session'),

  getRcAuthorizeUrl: (accountId: string) =>
    request<{ authorizeUrl: string; state: string }>(accountId, '/rc/oauth/authorize-url'),

  exchangeRcOAuthToken: (
    accountId: string,
    body: { code: string; state: string },
  ) =>
    request<{ status: string }>(accountId, '/rc/oauth/token', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};
