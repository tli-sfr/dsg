import { GlobeMd } from '@ringcentral/spring-icon';
import {
  Alert,
  Block,
  BlockHeader,
  Button,
  FormLabel,
  Icon,
  Option,
  Select,
  Tag,
  TextField,
} from '@ringcentral/spring-ui';
import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import type { DirectoryOAuthConfig, DirectoryType } from '../api/types';
import { useAccountId } from '../components/AccountBar';
import { ConnectionSuccessBanner } from '../components/ConnectionSuccessBanner';
import {
  DirectoryGroupPickerDialog,
  type DirectoryGroupOption,
} from '../components/DirectoryGroupPickerDialog';
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
  const [selectedGroupName, setSelectedGroupName] = useState('');
  const [groupPickerOpen, setGroupPickerOpen] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<{
    firstName: string | null;
    lastName: string | null;
  } | null>(null);
  const cleanupOAuthWait = useRef<(() => void) | null>(null);

  const connected = config?.connected ?? false;
  const provider = PROVIDERS.find((p) => p.value === directoryType);
  const formLocked = connected || connecting || busy;

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
      if (oauthConfig.clientId) {
        setClientId(oauthConfig.clientId);
      }
      if (oauthConfig.azureTenantId) {
        setAzureTenantId(oauthConfig.azureTenantId);
      }
      if (oauthConfig.oktaDomain) {
        setOktaDomain(oauthConfig.oktaDomain);
      }
      if (dir?.directoryGroupId) {
        setSelectedGroupId(dir.directoryGroupId);
        setSelectedGroupName(dir.directoryGroupName ?? '');
      } else {
        setSelectedGroupId('');
        setSelectedGroupName('');
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load configuration');
    }
  }, [accountId]);

  useEffect(() => {
    refresh();
    return () => {
      cleanupOAuthWait.current?.();
    };
  }, [refresh]);

  function showConnectionSuccessBanner(firstName: string | null, lastName: string | null) {
    setSuccessBanner({ firstName, lastName });
  }

  function endConnectAttempt() {
    setConnecting(false);
    cleanupOAuthWait.current = null;
  }

  async function handleConnect() {
    setConnecting(true);
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
      cleanupOAuthWait.current = waitForDirectoryOAuthResult(
        popup,
        accountId,
        async (user) => {
          let firstName = user.connectedUserFirstName;
          let lastName = user.connectedUserLastName;
          if (!firstName && !lastName) {
            const oauthConfig = await api.getDirectoryOAuthConfig(accountId);
            firstName = oauthConfig.connectedUserFirstName;
            lastName = oauthConfig.connectedUserLastName;
          }
          showConnectionSuccessBanner(firstName, lastName);
          setMessage('Directory connected successfully.');
          setClientSecret('');
          await refresh();
          endConnectAttempt();
        },
        (err) => {
          setError(err);
          endConnectAttempt();
        },
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Connect failed');
      endConnectAttempt();
    }
  }

  async function handleDisconnect() {
    setBusy(true);
    setError(null);
    setMessage(null);
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

  async function handleSelectGroup(group: DirectoryGroupOption) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await api.updateDirectory(accountId, {
        directoryGroupId: group.id,
        directoryGroupName: group.name,
        active: true,
      });
      setSelectedGroupId(group.id);
      setSelectedGroupName(group.name);
      setMessage('Directory group saved.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save group');
    } finally {
      setBusy(false);
    }
  }

  const groupDisplayValue = selectedGroupName || selectedGroupId;

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-6">
      {successBanner && (
        <ConnectionSuccessBanner
          firstName={successBanner.firstName}
          lastName={successBanner.lastName}
          onDismiss={() => setSuccessBanner(null)}
        />
      )}
      <div>
        <h1 className="typography-title text-neutral-b1">Directory Configuration</h1>
        <p className="typography-label text-neutral-b3">
          Connect your identity provider and choose which directory group to sync.
        </p>
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

      <Block>
        <BlockHeader
          divider
          endSlot={
            connected ? (
              <Tag color="success" variant="filled">
                CONNECTED
              </Tag>
            ) : connecting ? (
              <Tag color="warning" variant="filled">
                CONNECTING…
              </Tag>
            ) : undefined
          }
        >
          <div className="flex items-start gap-3">
            <Icon symbol={GlobeMd} className="text-primary-b" />
            <div>
              <div className="typography-subtitleMiniSemiBold">IDP Authorization</div>
              <p className="typography-label text-neutral-b3">
                Connect to your directory provider via OAuth
              </p>
            </div>
          </div>
        </BlockHeader>

        <div className="space-y-6">
          <Select
            label="Provider"
            className="w-full"
            value={directoryType}
            disabled={formLocked}
            onChange={(e) => setDirectoryType(e.target.value as DirectoryType)}
          >
            {PROVIDERS.map((p) => (
              <Option key={p.value} value={p.value}>
                {p.label}
              </Option>
            ))}
          </Select>

          <div className="flex flex-col gap-3">
            <FormLabel className="typography-descriptorMini uppercase text-neutral-b3">
              API Credentials
            </FormLabel>
            {directoryType === 'Azure' && (
              <TextField
                fullWidth
                label="Azure Tenant ID"
                placeholder="Azure Tenant ID"
                value={azureTenantId}
                disabled={formLocked}
                onChange={(e) => setAzureTenantId(e.target.value)}
              />
            )}
            {directoryType === 'Okta' && (
              <TextField
                fullWidth
                label="Okta domain"
                placeholder="https://dev-example.okta.com"
                value={oktaDomain}
                disabled={formLocked}
                onChange={(e) => setOktaDomain(e.target.value)}
              />
            )}
            <TextField
              fullWidth
              label="Client ID"
              placeholder="Client ID"
              value={clientId}
              disabled={formLocked}
              onChange={(e) => setClientId(e.target.value)}
            />
            {!connected && (
              <TextField
                fullWidth
                type="password"
                label="Client Secret"
                placeholder="Client Secret"
                value={clientSecret}
                disabled={connecting || busy}
                onChange={(e) => setClientSecret(e.target.value)}
              />
            )}
            {connected && (
              <p className="typography-descriptorMini text-neutral-b3">
                Client secret is stored securely. Disconnect to change credentials.
              </p>
            )}
          </div>

          <TextField
            fullWidth
            label="Callback URL"
            readOnly
            value={config?.callbackUrl ?? ''}
            helperText="Register this redirect URI in your IDP app configuration."
          />
        </div>

        <div className="mt-6 flex justify-end gap-3 border-t border-neutral-b4 pt-4">
          <Button
            variant="outlined"
            color="danger"
            disabled={!connected || connecting || busy}
            onClick={handleDisconnect}
          >
            Disconnect
          </Button>
          <Button
            variant="contained"
            disabled={connected || connecting || busy || !provider?.supportsOAuth}
            onClick={handleConnect}
          >
            {connecting ? 'Connecting…' : connected ? 'Connected' : 'Connect'}
          </Button>
        </div>
      </Block>

      {connected && (
        <Block>
          <BlockHeader divider>
            <div>
              <div className="typography-subtitleMiniSemiBold">Directory group</div>
              <p className="typography-label text-neutral-b3">
                Choose the directory group whose users will be provisioned to RingCentral.
              </p>
            </div>
          </BlockHeader>
          <div className="flex flex-wrap items-end gap-3">
            <TextField
              fullWidth
              label="Group"
              readOnly
              disabled
              value={groupDisplayValue}
              placeholder="Please select a directory group for user provisioning"
              RootProps={{ className: 'min-w-[16rem] flex-1' }}
            />
            <Button variant="outlined" color="secondary" disabled={busy} onClick={() => setGroupPickerOpen(true)}>
              Group
            </Button>
          </div>
        </Block>
      )}

      <DirectoryGroupPickerDialog
        accountId={accountId}
        open={groupPickerOpen}
        onClose={() => setGroupPickerOpen(false)}
        onSelect={(group) => void handleSelectGroup(group)}
      />
    </div>
  );
}
