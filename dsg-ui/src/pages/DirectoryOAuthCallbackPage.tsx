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
      notifyOpener({ type: 'directory-oauth-error', message: oauthError });
      return;
    }
    if (!code || !state) {
      const err = 'Missing authorization code or state.';
      setMessage(err);
      notifyOpener({ type: 'directory-oauth-error', message: err });
      return;
    }

    const normalizedState = decodeURIComponent(state);
    const accountId = accountIdFromState(normalizedState);
    const exchangeKey = `directory_oauth_exchanged_${code}`;

    if (!accountId) {
      const err = 'Could not determine account ID from OAuth state.';
      setMessage(err);
      notifyOpener({ type: 'directory-oauth-error', message: err });
      return;
    }
    const resolvedAccountId = accountId;

    let ignoreStateUpdates = false;
    let pollId: number | undefined;

    function finishSuccess(user: {
      connectedUserFirstName: string | null;
      connectedUserLastName: string | null;
    }) {
      sessionStorage.removeItem('directory_oauth_state');
      if (!ignoreStateUpdates) {
        setMessage('Connected successfully. Closing…');
      }
      notifyOpener({
        type: 'directory-oauth-success',
        connectedUserFirstName: user.connectedUserFirstName,
        connectedUserLastName: user.connectedUserLastName,
      });
      window.setTimeout(() => window.close(), 400);
    }

    function finishError(err: string) {
      sessionStorage.removeItem(exchangeKey);
      if (!ignoreStateUpdates) {
        setMessage(err);
      }
      notifyOpener({ type: 'directory-oauth-error', message: err });
    }

    function readStoredUser(): {
      connectedUserFirstName: string | null;
      connectedUserLastName: string | null;
    } {
      return {
        connectedUserFirstName: sessionStorage.getItem('directory_oauth_user_first'),
        connectedUserLastName: sessionStorage.getItem('directory_oauth_user_last'),
      };
    }

    function storeUserNames(
      firstName: string | null | undefined,
      lastName: string | null | undefined,
    ) {
      if (firstName) {
        sessionStorage.setItem('directory_oauth_user_first', firstName);
      }
      if (lastName) {
        sessionStorage.setItem('directory_oauth_user_last', lastName);
      }
    }

    async function waitForExchange() {
      pollId = window.setInterval(async () => {
        if (sessionStorage.getItem(exchangeKey) === 'done') {
          if (pollId !== undefined) window.clearInterval(pollId);
          finishSuccess(readStoredUser());
          return;
        }
        try {
          const status = await api.getDirectoryOAuthConfig(resolvedAccountId);
          if (status.connected) {
            sessionStorage.setItem(exchangeKey, 'done');
            storeUserNames(status.connectedUserFirstName, status.connectedUserLastName);
            if (pollId !== undefined) window.clearInterval(pollId);
            finishSuccess({
              connectedUserFirstName: status.connectedUserFirstName,
              connectedUserLastName: status.connectedUserLastName,
            });
          }
        } catch {
          /* keep polling */
        }
      }, 400);
    }

    if (sessionStorage.getItem(exchangeKey) === 'done') {
      finishSuccess(readStoredUser());
      return;
    }

    async function run() {
      try {
        const status = await api.getDirectoryOAuthConfig(resolvedAccountId);
        if (status.connected) {
          sessionStorage.setItem(exchangeKey, 'done');
          storeUserNames(status.connectedUserFirstName, status.connectedUserLastName);
          finishSuccess({
            connectedUserFirstName: status.connectedUserFirstName,
            connectedUserLastName: status.connectedUserLastName,
          });
          return;
        }
      } catch {
        /* proceed with exchange */
      }

      if (sessionStorage.getItem(exchangeKey) === 'in_progress') {
        waitForExchange();
        return;
      }

      sessionStorage.setItem(exchangeKey, 'in_progress');
      try {
        const result = await api.exchangeDirectoryOAuthToken(resolvedAccountId, {
          code: code!,
          state: normalizedState,
        });
        sessionStorage.setItem(exchangeKey, 'done');
        storeUserNames(result.connectedUserFirstName, result.connectedUserLastName);
        finishSuccess({
          connectedUserFirstName: result.connectedUserFirstName,
          connectedUserLastName: result.connectedUserLastName,
        });
      } catch (e) {
        finishError(e instanceof Error ? e.message : 'Token exchange failed');
      }
    }

    run();
    return () => {
      ignoreStateUpdates = true;
      if (pollId !== undefined) {
        window.clearInterval(pollId);
      }
    };
  }, [code, oauthError, state]);

  return (
    <div className="mx-auto max-w-lg p-8">
      <h1 className="typography-title text-neutral-b1">Directory OAuth</h1>
      <p className="mt-4 typography-label text-neutral-b2">{message}</p>
    </div>
  );
}

function notifyOpener(payload: {
  type: string;
  message?: string;
  connectedUserFirstName?: string | null;
  connectedUserLastName?: string | null;
}) {
  if (window.opener && !window.opener.closed) {
    window.opener.postMessage(payload, window.location.origin);
  }
}

function accountIdFromState(state: string): string | null {
  const firstColon = state.indexOf(':');
  return firstColon > 0 ? state.substring(0, firstColon) : null;
}
