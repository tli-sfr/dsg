export type DirectoryType = 'Azure' | 'Okta' | 'Google' | 'OneLogin';
export type JobType = 'FULL' | 'INCREMENTAL' | 'ON_DEMAND';
export type DeprovisioningType = 'FULL_DELETE' | 'RECLAIM_RESOURCE' | 'DISABLE_ONLY';

export interface DirectoryResponse {
  directoryType: string;
  directoryGroupId: string | null;
  directoryGroupName: string | null;
  active: boolean;
  connected: boolean;
}

export interface DirectoryOAuthConfig {
  directoryType: string | null;
  authFlow: string | null;
  clientId: string | null;
  azureTenantId: string | null;
  oktaDomain: string | null;
  callbackUrl: string;
  connected: boolean;
  tokenExpiresAt: string | null;
  connectedUserFirstName: string | null;
  connectedUserLastName: string | null;
}

export interface JobReportResponse {
  jobId: string;
  jobType: string;
  syncDirection: string;
  state: string;
  startedAt: string;
  completedAt: string | null;
  successCount: number;
  failedCount: number;
  failures: { externalId: string; operation: string; comment: string }[];
}

export interface JobSummary {
  jobId: string;
  jobType: string;
  syncDirection: string;
  state: string;
  startedAt: string;
  completedAt: string | null;
  successCount: number;
  failedCount: number;
}

export interface ProvisioningRuleSummary {
  ruleId: string;
  ruleName: string;
  priority: number;
  selectionExpression: Record<string, unknown>;
}

export interface ProvisioningRuleDetail {
  ruleId: string;
  ruleName: string;
  priority: number;
  selectionExpression: Record<string, unknown>;
  licenseAssignments: { licenseType: string; licenseId: string }[];
  areaCodeAssignment: { areaCodeRuleType: string; areaCodeList: string[] } | null;
  deviceAssignments: { deviceType: string; deviceId?: string }[];
  templateAssignments: { templateType: string; templateId: string }[];
  ruleBasedAttributeMappings: unknown[];
}

export interface SelectionCriterion {
  attribute: string;
  operator: string;
  value: string;
}

export interface ApiError {
  code: string;
  message: string;
}

export interface AttributeMappingItem {
  directoryAttributePath: string;
  directoryAttributeName: string;
  rcAttribute: string;
  displaySequence: number;
}

export interface AttributeCatalogItem {
  attributeName: string;
  attributePath: string;
  displayName: string;
}

export interface AttributeMappingConfig {
  syncDirection: string;
  accountConfigured: boolean;
  mappings: AttributeMappingItem[];
  directoryAttributes: AttributeCatalogItem[];
  rcAttributes: AttributeCatalogItem[];
}

export interface AttributeMappingRow {
  syncDirection: string;
  directoryAttribute: string;
  rcAttribute: string;
}
