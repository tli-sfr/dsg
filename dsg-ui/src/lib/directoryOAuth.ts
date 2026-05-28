const POPUP_WIDTH = 520;
const POPUP_HEIGHT = 720;

export function openDirectoryOAuthPopup(authorizeUrl: string, state: string): Window | null {
  sessionStorage.setItem('directory_oauth_state', state);
  const left = window.screenX + (window.outerWidth - POPUP_WIDTH) / 2;
  const top = window.screenY + (window.outerHeight - POPUP_HEIGHT) / 2;
  return window.open(
    authorizeUrl,
    'dsg-directory-oauth',
    `width=${POPUP_WIDTH},height=${POPUP_HEIGHT},left=${left},top=${top},noopener,noreferrer`,
  );
}

export function waitForDirectoryOAuthResult(
  popup: Window | null,
  onComplete: () => void,
  onError: (message: string) => void,
): () => void {
  const timer = window.setInterval(() => {
    if (popup && popup.closed) {
      window.clearInterval(timer);
      onComplete();
    }
  }, 500);

  function handleMessage(event: MessageEvent) {
    if (event.origin !== window.location.origin) return;
    if (event.data?.type === 'directory-oauth-success') {
      window.clearInterval(timer);
      window.removeEventListener('message', handleMessage);
      if (popup && !popup.closed) popup.close();
      onComplete();
    }
    if (event.data?.type === 'directory-oauth-error') {
      window.clearInterval(timer);
      window.removeEventListener('message', handleMessage);
      if (popup && !popup.closed) popup.close();
      onError(event.data.message ?? 'Directory OAuth failed');
    }
  }

  window.addEventListener('message', handleMessage);
  return () => {
    window.clearInterval(timer);
    window.removeEventListener('message', handleMessage);
  };
}
