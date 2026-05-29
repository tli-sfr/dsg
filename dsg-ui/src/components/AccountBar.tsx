import { Button, TextField } from '@ringcentral/spring-ui';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { logoutAndRedirectToRcOAuthLogin } from '../lib/rcOAuth';

export function AccountBar() {
  const [params, setParams] = useSearchParams();
  const urlAccountId =
    params.get('accountId') ??
    import.meta.env.VITE_DEFAULT_ACCOUNT_ID ??
    'demo-acct';
  const [displayAccountId, setDisplayAccountId] = useState(urlAccountId);
  const [extensionLabel, setExtensionLabel] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  useEffect(() => {
    setDisplayAccountId(urlAccountId);
    let cancelled = false;

    async function resolveRcAccount() {
      setLoading(true);
      setLogoutError(null);
      try {
        const status = await api.getRcOAuthStatus(urlAccountId);
        if (cancelled) return;

        setConnected(status.connected);
        if (!status.connected) {
          setExtensionLabel(null);
          return;
        }

        const session = await api.getRcOAuthSession(urlAccountId);
        if (cancelled) return;

        setDisplayAccountId(session.rcAccountId);
        if (session.extensionName) {
          setExtensionLabel(`${session.extensionName} (ext. ${session.extensionNumber ?? '—'})`);
        } else if (session.extensionNumber) {
          setExtensionLabel(`ext. ${session.extensionNumber}`);
        } else {
          setExtensionLabel(null);
        }

        if (session.rcAccountId !== urlAccountId) {
          setParams({ accountId: session.rcAccountId }, { replace: true });
        }
      } catch {
        if (!cancelled) {
          setConnected(false);
          setExtensionLabel(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    resolveRcAccount();
    return () => {
      cancelled = true;
    };
  }, [urlAccountId, setParams]);

  async function handleLogout() {
    setLoggingOut(true);
    setLogoutError(null);
    try {
      await logoutAndRedirectToRcOAuthLogin(displayAccountId);
    } catch (e) {
      setLoggingOut(false);
      setLogoutError(e instanceof Error ? e.message : 'Logout failed');
    }
  }

  return (
    <div className="border-b border-neutral-b4 bg-neutral-base px-6 py-3">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 flex-wrap items-center gap-3">
          <span className="typography-labelSemiBold text-neutral-b2">Account ID</span>
          <TextField
            size="medium"
            readOnly
            aria-label="RingCentral account ID"
            value={loading ? 'Loading…' : displayAccountId}
            RootProps={{ className: 'max-w-xs' }}
          />
          {extensionLabel && (
            <span className="typography-label text-neutral-b3">{extensionLabel}</span>
          )}
        </div>
        {connected && (
          <Button
            variant="outlined"
            color="primary"
            size="small"
            disabled={loggingOut}
            onClick={() => void handleLogout()}
          >
            {loggingOut ? 'Logging out…' : 'Log out'}
          </Button>
        )}
      </div>
      {logoutError && (
        <p className="mt-2 typography-descriptorMini text-danger-b">{logoutError}</p>
      )}
    </div>
  );
}

export function useAccountId(): string {
  const [params] = useSearchParams();
  return params.get('accountId') ?? import.meta.env.VITE_DEFAULT_ACCOUNT_ID ?? 'demo-acct';
}
