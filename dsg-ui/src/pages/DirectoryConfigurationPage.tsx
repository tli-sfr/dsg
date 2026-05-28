import { Database, Globe, KeyRound } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { DirectoryOAuthConfig, DirectoryType } from '../api/types';
import { useAccountId } from '../components/AccountBar';
import { Card } from '../components/Card';
import { openDirectoryOAuthPopup, waitForDirectoryOAuthResult } from '../lib/directoryOAuth';

const PROVIDERS: { value: DirectoryType; label: string; supportsOAuth: boolean }[] = [
  { value: 'Azure', label: 'Microsoft Azure AD (Entra ID)', supportsOAuth: true },
  { value: 'Okta', label: 'Okta', supportsOAuth: true },
  { value: 'Google', label: 'Google Workspace', supportsOAuth: false },
  { value: 'OneLogin', label: 'OneLogin', supportsOAuth: false },
];

export function DirectoryConfigurationPage() {
  const accountId = useAccountId();
  const [config, setConfig] = useState<DirectoryOAuthConfig | null>(null);
  const [directoryType, setDirectoryType] = useState<DirectoryType>('Azure');
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [azureTenantId, setAzureTenantId] = useState('');
  const [oktaDomain, setOktaDomain] = useState('');
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [groups, setGroups] = useState<{ id: string; name: string }[]>([]);
  const [directory, setDirectory] = useState<{ directoryGroupId: string | null; active: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const connected = config?.connected ?? false;
  const provider = PROVIDERS.find((p) => p.value === directoryType);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [oauthConfig, dir] = await Promise.all([
        api.getDirectoryOAuthConfig(accountId),
        api.getDirectory(accountId).catch(() => null),
      ]);
      setConfig(oauthConfig);
      if (oauthConfig.directoryType) {
        setDirectoryType(oauthConfig.directoryType as DirectoryType);
      }
      setDirectory(dir);
      if (dir?.directoryGroupId) {
        setSelectedGroupId(dir.directoryGroupId);
      }
      if (oauthConfig.connected) {
        const groupList = await api.listDirectoryGroups(accountId);
        setGroups(groupList.groups);
      } else {
        setGroups([]);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load configuration');
    }
  }, [accountId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleConnect() {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      if (!clientId.trim() || !clientSecret.trim()) {
        throw new Error('Client ID and Client Secret are required.');
      }
      if (directoryType === 'Azure' && !azureTenantId.trim()) {
        throw new Error('Azure tenant ID is required.');
      }
      if (directoryType === 'Okta' && !oktaDomain.trim()) {
        throw new Error('Okta domain is required (e.g. https://dev-example.okta.com).');
      }
      if (!provider?.supportsOAuth) {
        throw new Error(`${directoryType} 3-legged OAuth is not available yet.`);
      }

      await api.saveDirectoryOAuth(accountId, {
        directoryType,
        authFlow: 'AUTHORIZATION_CODE',
        clientId: clientId.trim(),
        clientSecret: clientSecret.trim(),
        azureTenantId: azureTenantId.trim() || undefined,
        oktaDomain: oktaDomain.trim() || undefined,
      });

      const { authorizeUrl, state } = await api.getDirectoryAuthorizeUrl(accountId);
      const popup = openDirectoryOAuthPopup(authorizeUrl, state);
      waitForDirectoryOAuthResult(
        popup,
        async () => {
          setMessage('Directory connected successfully.');
          setClientSecret('');
          await refresh();
          setBusy(false);
        },
        (err) => {
          setError(err);
          setBusy(false);
        },
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Connect failed');
      setBusy(false);
    }
  }

  async function handleDisconnect() {
    setBusy(true);
    setError(null);
    try {
      await api.disconnectDirectoryOAuth(accountId);
      setMessage('Directory disconnected.');
      setClientSecret('');
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Disconnect failed');
    } finally {
      setBusy(false);
    }
  }

  async function handleSaveGroup() {
    if (!selectedGroupId) return;
    setBusy(true);
    setError(null);
    try {
      await api.updateDirectory(accountId, { directoryGroupId: selectedGroupId, active: true });
      setMessage('Directory group saved.');
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save group');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-semibold text-rc-navy">Directory Configuration</h1>
        <p className="text-sm text-slate-500">
          Connect your identity provider and choose which directory group to sync.
        </p>
      </div>

      {message && <p className="rounded bg-green-50 px-4 py-2 text-sm text-green-800">{message}</p>}
      {error && <p className="rounded bg-red-50 px-4 py-2 text-sm text-red-800">{error}</p>}

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <header className="flex items-start justify-between border-b border-slate-100 px-6 py-5">
          <div className="flex items-start gap-3">
            <div className="rounded-lg bg-blue-50 p-2 text-blue-600">
              <Globe className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-slate-900">IDP Authorization</h2>
              <p className="text-sm text-slate-500">Connect to your directory provider via OAuth</p>
            </div>
          </div>
          {connected && (
            <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-semibold tracking-wide text-green-700">
              CONNECTED
            </span>
          )}
        </header>

        <div className="space-y-6 px-6 py-5">
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Provider
            </label>
            <div className="relative">
              <Database className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <select
                className="w-full appearance-none rounded-lg border border-slate-300 py-2.5 pl-10 pr-4 text-sm"
                value={directoryType}
                disabled={busy}
                onChange={(e) => setDirectoryType(e.target.value as DirectoryType)}
              >
                {PROVIDERS.map((p) => (
                  <option key={p.value} value={p.value}>
                    {p.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              API Credentials
            </label>
            <div className="space-y-3">
              {directoryType === 'Azure' && (
                <div className="relative">
                  <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-4 text-sm"
                    placeholder="Azure Tenant ID"
                    value={azureTenantId}
                    disabled={busy}
                    onChange={(e) => setAzureTenantId(e.target.value)}
                  />
                </div>
              )}
              {directoryType === 'Okta' && (
                <div className="relative">
                  <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-4 text-sm"
                    placeholder="Okta domain (https://dev-example.okta.com)"
                    value={oktaDomain}
                    disabled={busy}
                    onChange={(e) => setOktaDomain(e.target.value)}
                  />
                </div>
              )}
              <div className="relative">
                <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-4 text-sm"
                  placeholder="Client ID"
                  value={clientId}
                  disabled={busy}
                  onChange={(e) => setClientId(e.target.value)}
                />
              </div>
              <div className="relative">
                <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="password"
                  className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-4 text-sm"
                  placeholder="Client Secret"
                  value={clientSecret}
                  disabled={busy}
                  onChange={(e) => setClientSecret(e.target.value)}
                />
              </div>
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Callback URL
            </label>
            <input
              className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2.5 px-4 text-sm text-slate-600"
              value={config?.callbackUrl ?? ''}
              readOnly
            />
            <p className="mt-1 text-xs text-slate-500">
              Register this redirect URI in your IDP app configuration.
            </p>
          </div>
        </div>

        <footer className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">
          <button
            type="button"
            disabled={busy || !connected}
            onClick={handleDisconnect}
            className="rounded-lg border border-red-300 px-5 py-2 text-sm font-medium text-red-600 disabled:opacity-40"
          >
            Disconnect
          </button>
          <button
            type="button"
            disabled={busy || !provider?.supportsOAuth}
            onClick={handleConnect}
            className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white disabled:opacity-40"
          >
            {busy ? 'Connecting…' : 'Connect'}
          </button>
        </footer>
      </section>

      {connected && (
        <Card title="Directory group">
          <p className="mb-4 text-sm text-slate-600">
            Select the directory group whose users will be synced to RingCentral.
          </p>
          <div className="flex flex-wrap items-end gap-4">
            <label className="min-w-[16rem] flex-1 text-sm">
              Group
              <select
                className="mt-1 block w-full rounded border border-slate-300 px-3 py-2"
                value={selectedGroupId}
                disabled={busy}
                onChange={(e) => setSelectedGroupId(e.target.value)}
              >
                <option value="">Select a group…</option>
                {groups.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              disabled={busy || !selectedGroupId}
              onClick={handleSaveGroup}
              className="rounded bg-rc-orange px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Save group
            </button>
          </div>
          {directory?.active && directory.directoryGroupId && (
            <p className="mt-3 text-sm text-green-700">
              Active sync group: <span className="font-medium">{directory.directoryGroupId}</span>
            </p>
          )}
        </Card>
      )}
    </div>
  );
}
