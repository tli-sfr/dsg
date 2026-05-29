import { Alert, CircularProgressIndicator } from '@ringcentral/spring-ui';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';

/**
 * RingCentral 3LO callback — exchanges authorization code on the Java server.
 */
export function OAuthCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const code = params.get('code');
  const state = params.get('state');
  const oauthError = params.get('error');
  const [status, setStatus] = useState<'pending' | 'success' | 'error'>('pending');
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (oauthError) {
      setStatus('error');
      setMessage(oauthError);
      return;
    }
    if (!code || !state) {
      setStatus('error');
      setMessage('Missing authorization code or state in callback URL.');
      return;
    }

    const normalizedState = decodeURIComponent(state);
    const resolvedAccountId =
      sessionStorage.getItem('rc_oauth_account_id') ||
      accountIdFromState(normalizedState) ||
      '';
    const storedState = sessionStorage.getItem('rc_oauth_state');
    const dashboardPath = `/directory-integration?accountId=${encodeURIComponent(resolvedAccountId)}`;
    const exchangeKey = `rc_oauth_exchanged_${code}`;

    if (!resolvedAccountId) {
      setStatus('error');
      setMessage('Could not determine account ID from OAuth state.');
      return;
    }

    if (storedState && storedState !== normalizedState) {
      setStatus('error');
      setMessage('OAuth state mismatch — possible CSRF attempt.');
      return;
    }

    let ignoreStateUpdates = false;
    let pollId: number | undefined;

    function goToDashboard() {
      sessionStorage.removeItem('rc_oauth_state');
      sessionStorage.removeItem('rc_oauth_account_id');
      navigate(dashboardPath, { replace: true });
    }

    function waitForConnection() {
      pollId = window.setInterval(async () => {
        if (sessionStorage.getItem(exchangeKey) === 'done') {
          if (pollId !== undefined) window.clearInterval(pollId);
          goToDashboard();
          return;
        }
        try {
          const oauthStatus = await api.getRcOAuthStatus(resolvedAccountId);
          if (oauthStatus.connected) {
            sessionStorage.setItem(exchangeKey, 'done');
            if (pollId !== undefined) window.clearInterval(pollId);
            goToDashboard();
          }
        } catch {
          /* keep polling */
        }
      }, 400);
    }

    if (sessionStorage.getItem(exchangeKey) === 'done') {
      goToDashboard();
      return;
    }

    async function run() {
      try {
        const oauthStatus = await api.getRcOAuthStatus(resolvedAccountId);
        if (oauthStatus.connected) {
          sessionStorage.setItem(exchangeKey, 'done');
          goToDashboard();
          return;
        }
      } catch {
        /* proceed with exchange */
      }

      if (sessionStorage.getItem(exchangeKey) === 'in_progress') {
        waitForConnection();
        return;
      }

      sessionStorage.setItem(exchangeKey, 'in_progress');
      try {
        await api.exchangeRcOAuthToken(resolvedAccountId, {
          code: code!,
          state: normalizedState,
        });
        sessionStorage.setItem(exchangeKey, 'done');
        if (!ignoreStateUpdates) {
          setStatus('success');
        }
        goToDashboard();
      } catch (e) {
        sessionStorage.removeItem(exchangeKey);
        if (!ignoreStateUpdates) {
          setStatus('error');
          setMessage(e instanceof Error ? e.message : 'Token exchange failed');
        }
      }
    }

    run();
    return () => {
      ignoreStateUpdates = true;
      if (pollId !== undefined) {
        window.clearInterval(pollId);
      }
    };
  }, [code, navigate, oauthError, state]);

  return (
    <div className="mx-auto max-w-lg p-8">
      <h1 className="typography-title text-neutral-b1">OAuth callback</h1>
      {status === 'pending' && (
        <div className="mt-4 flex items-center gap-3">
          <CircularProgressIndicator size="small" />
          <p className="typography-label text-neutral-b2">Exchanging authorization code…</p>
        </div>
      )}
      {status === 'success' && (
        <Alert severity="success" className="mt-4">
          RingCentral login successful. Redirecting to dashboard…
        </Alert>
      )}
      {status === 'error' && (
        <Alert severity="error" className="mt-4">
          Error: {message}
        </Alert>
      )}
    </div>
  );
}

function accountIdFromState(state: string): string | null {
  const colon = state.indexOf(':');
  return colon > 0 ? state.substring(0, colon) : null;
}
