import { api } from '../api/client';

export function clearRcOAuthBrowserState() {
  sessionStorage.removeItem('rc_oauth_state');
  sessionStorage.removeItem('rc_oauth_account_id');
}

export async function redirectToRcOAuthLogin(accountId: string) {
  const { authorizeUrl, state } = await api.getRcAuthorizeUrl(accountId);
  sessionStorage.setItem('rc_oauth_state', state);
  sessionStorage.setItem('rc_oauth_account_id', accountId);
  window.location.assign(authorizeUrl);
}

export async function logoutAndRedirectToRcOAuthLogin(accountId: string) {
  await api.logoutRcOAuth(accountId);
  clearRcOAuthBrowserState();
  await redirectToRcOAuthLogin(accountId);
}
