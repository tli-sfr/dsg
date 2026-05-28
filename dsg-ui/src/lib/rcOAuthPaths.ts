/** Paths registered as RingCentral OAuth redirect URIs (must match backend dsg.rc.redirect-uri). */
export const RC_OAUTH_CALLBACK_PATHS = ['/oauth/callback', '/mobile/oauthredirect'];

export function isRcOAuthCallbackPath(pathname: string): boolean {
  return RC_OAUTH_CALLBACK_PATHS.includes(pathname);
}
