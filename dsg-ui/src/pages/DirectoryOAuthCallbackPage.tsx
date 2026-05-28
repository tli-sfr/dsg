import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';

/**
 * Directory IDP OAuth popup callback — exchanges code on the Java server.
 */
export function DirectoryOAuthCallbackPage() {
  const [params] = useSearchParams();
  const code = params.get('code');
  const state = params.get('state');
  const oauthError = params.get('error');
  const [message, setMessage] = useState('Completing directory login…');

  useEffect(() => {
    if (oauthError) {
      setMessage(oauthError);
      window.opener?.postMessage(
        { type: 'directory-oauth-error', message: oauthError },
        window.location.origin,
      );
      return;
    }
    if (!code || !state) {
      const err = 'Missing authorization code or state.';
      setMessage(err);
      window.opener?.postMessage({ type: 'directory-oauth-error', message: err }, window.location.origin);
      return;
    }

    const normalizedState = decodeURIComponent(state);
    const accountId = accountIdFromState(normalizedState);
    const storedState = sessionStorage.getItem('directory_oauth_state');

    if (!accountId) {
      const err = 'Could not determine account ID from OAuth state.';
      setMessage(err);
      window.opener?.postMessage({ type: 'directory-oauth-error', message: err }, window.location.origin);
      return;
    }

    if (storedState && storedState !== normalizedState) {
      const err = 'OAuth state mismatch.';
      setMessage(err);
      window.opener?.postMessage({ type: 'directory-oauth-error', message: err }, window.location.origin);
      return;
    }

    let cancelled = false;

    async function exchange() {
      try {
        await api.exchangeDirectoryOAuthToken(accountId!, { code, state: normalizedState });
        if (cancelled) return;
        sessionStorage.removeItem('directory_oauth_state');
        setMessage('Connected. You can close this window.');
        window.opener?.postMessage({ type: 'directory-oauth-success' }, window.location.origin);
        window.close();
      } catch (e) {
        if (cancelled) return;
        const err = e instanceof Error ? e.message : 'Token exchange failed';
        setMessage(err);
        window.opener?.postMessage({ type: 'directory-oauth-error', message: err }, window.location.origin);
      }
    }

    exchange();
    return () => {
      cancelled = true;
    };
  }, [code, oauthError, state]);

  return (
    <div className="mx-auto max-w-lg p-8">
      <h1 className="text-xl font-semibold text-rc-navy">Directory OAuth</h1>
      <p className="mt-4 text-sm text-slate-600">{message}</p>
    </div>
  );
}

function accountIdFromState(state: string): string | null {
  const firstColon = state.indexOf(':');
  return firstColon > 0 ? state.substring(0, firstColon) : null;
}
