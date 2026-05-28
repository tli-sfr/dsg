/** Paths registered as directory IDP OAuth redirect URIs. */
export const DIRECTORY_OAUTH_CALLBACK_PATH = '/directory-integration/oauth/callback';

export function isDirectoryOAuthCallbackPath(pathname: string): boolean {
  return pathname === DIRECTORY_OAUTH_CALLBACK_PATH;
}
