import { api } from '../api/client';

const POPUP_WIDTH = 520;
const POPUP_HEIGHT = 720;

export function openDirectoryOAuthPopup(authorizeUrl: string, state: string): Window | null {
  sessionStorage.setItem('directory_oauth_state', state);
  const left = window.screenX + (window.outerWidth - POPUP_WIDTH) / 2;
  const top = window.screenY + (window.outerHeight - POPUP_HEIGHT) / 2;
  // Do not use noopener — the callback page must postMessage back to window.opener.
  return window.open(
    authorizeUrl,
    'dsg-directory-oauth',
    `width=${POPUP_WIDTH},height=${POPUP_HEIGHT},left=${left},top=${top}`,
  );
}

export type DirectoryOAuthSuccess = {
  connectedUserFirstName: string | null;
  connectedUserLastName: string | null;
};

export function waitForDirectoryOAuthResult(
  popup: Window | null,
  accountId: string,
  onSuccess: (user: DirectoryOAuthSuccess) => void,
  onError: (message: string) => void,
): () => void {
  let finished = false;

  function cleanup(timer: number, onMessage: (event: MessageEvent) => void) {
    window.clearInterval(timer);
    window.removeEventListener('message', onMessage);
  }

  async function verifyConnectedOrFail() {
    try {
      const status = await api.getDirectoryOAuthConfig(accountId);
      if (status.connected) {
        finishSuccess({
          connectedUserFirstName: status.connectedUserFirstName,
          connectedUserLastName: status.connectedUserLastName,
        });
      } else {
        finishError('Directory authentication was not completed. Please try again.');
      }
    } catch {
      finishError('Directory authentication was not completed. Please try again.');
    }
  }

  function finishSuccess(user: DirectoryOAuthSuccess) {
    if (finished) return;
    finished = true;
    onSuccess(user);
  }

  function finishError(message: string) {
    if (finished) return;
    finished = true;
    onError(message);
  }

  const timer = window.setInterval(() => {
    if (popup && popup.closed) {
      cleanup(timer, onMessage);
      void verifyConnectedOrFail();
    }
  }, 400);

  function onMessage(event: MessageEvent) {
    if (event.origin !== window.location.origin) return;
    if (event.data?.type === 'directory-oauth-success') {
      cleanup(timer, onMessage);
      if (popup && !popup.closed) popup.close();
      finishSuccess({
        connectedUserFirstName: event.data.connectedUserFirstName ?? null,
        connectedUserLastName: event.data.connectedUserLastName ?? null,
      });
    }
    if (event.data?.type === 'directory-oauth-error') {
      cleanup(timer, onMessage);
      if (popup && !popup.closed) popup.close();
      finishError(event.data.message ?? 'Directory OAuth failed');
    }
  }

  window.addEventListener('message', onMessage);

  if (!popup) {
    cleanup(timer, onMessage);
    finishError('Popup blocked. Allow pop-ups for this site and try again.');
  }

  return () => {
    if (!finished) {
      finished = true;
      cleanup(timer, onMessage);
    }
  };
}
