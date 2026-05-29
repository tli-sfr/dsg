import { Alert, CircularProgressIndicator } from '@ringcentral/spring-ui';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { api } from '../api/client';
import { isDirectoryOAuthCallbackPath } from '../lib/directoryOAuthPaths';
import { redirectToRcOAuthLogin } from '../lib/rcOAuth';
import { isRcOAuthCallbackPath } from '../lib/rcOAuthPaths';
import { useAccountId } from './AccountBar';

type AuthState = 'loading' | 'connected' | 'redirecting' | 'not-configured' | 'error';

/**
 * Ensures the user has completed RingCentral 3-legged OAuth before using the admin UI.
 */
export function RcAuthGuard({ children }: { children: React.ReactNode }) {
  const accountId = useAccountId();
  const location = useLocation();
  const [authState, setAuthState] = useState<AuthState>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const onOAuthCallback =
    isRcOAuthCallbackPath(location.pathname)
    || isDirectoryOAuthCallbackPath(location.pathname)
    || location.search.includes('code=');

  useEffect(() => {
    if (onOAuthCallback) {
      return;
    }

    let cancelled = false;

    async function ensureAuthenticated() {
      setAuthState('loading');
      setErrorMessage(null);
      try {
        const status = await api.getRcOAuthStatus(accountId);
        if (cancelled) return;

        if (!status.configured) {
          setAuthState('not-configured');
          return;
        }
        if (status.connected) {
          setAuthState('connected');
          return;
        }

        if (cancelled) return;

        setAuthState('redirecting');
        await redirectToRcOAuthLogin(accountId);
      } catch (e) {
        if (cancelled) return;
        setAuthState('error');
        setErrorMessage(e instanceof Error ? e.message : 'Authentication check failed');
      }
    }

    ensureAuthenticated();
    return () => {
      cancelled = true;
    };
  }, [accountId, location.pathname, location.search, onOAuthCallback]);

  if (onOAuthCallback) {
    return <>{children}</>;
  }

  if (authState === 'connected') {
    return <>{children}</>;
  }

  if (authState === 'loading' || authState === 'redirecting') {
    return (
      <div className="mx-auto flex max-w-lg flex-col items-center gap-4 p-8 text-center">
        <CircularProgressIndicator />
        <p className="typography-label text-neutral-b2">
          {authState === 'redirecting'
            ? 'Redirecting to RingCentral login…'
            : 'Checking RingCentral authentication…'}
        </p>
      </div>
    );
  }

  if (authState === 'not-configured') {
    return (
      <div className="mx-auto max-w-lg space-y-3 p-8">
        <h1 className="typography-title text-neutral-b1">RingCentral OAuth not configured</h1>
        <p className="typography-label text-neutral-b2">
          Copy <code className="rounded-sui-sm bg-neutral-b5 px-1">dsg-api/src/main/resources/application-local.yml.example</code>{' '}
          to <code className="rounded-sui-sm bg-neutral-b5 px-1">application-local.yml</code>, add your Client ID and Client
          Secret, then restart the backend with profile <code className="rounded-sui-sm bg-neutral-b5 px-1">local</code>.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg p-8">
      <Alert severity="error">{errorMessage ?? 'Authentication failed'}</Alert>
    </div>
  );
}
