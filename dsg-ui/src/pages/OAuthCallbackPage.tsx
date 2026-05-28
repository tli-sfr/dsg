import { Link, useSearchParams } from 'react-router-dom';

/**
 * Standalone RC 3LO callback — exchange code via RingCentral token endpoint
 * when VITE_RC_CLIENT_ID is configured. See docs/api/rc-oauth-dev-setup.md.
 */
export function OAuthCallbackPage() {
  const [params] = useSearchParams();
  const code = params.get('code');
  const oauthError = params.get('error');

  return (
    <div className="mx-auto max-w-lg p-8">
      <h1 className="text-xl font-semibold text-rc-navy">OAuth callback</h1>
      {oauthError && (
        <p className="mt-4 text-red-700">Error: {oauthError}</p>
      )}
      {code && (
        <p className="mt-4 text-sm text-slate-600">
          Authorization code received. Wire token exchange with{' '}
          <code className="rounded bg-slate-100 px-1">VITE_RC_CLIENT_ID</code> per{' '}
          <code>docs/api/rc-oauth-dev-setup.md</code>.
        </p>
      )}
      {!code && !oauthError && (
        <p className="mt-4 text-sm text-slate-500">No authorization code in query string.</p>
      )}
      <Link to="/directory-integration" className="mt-6 inline-block text-rc-orange">
        Back to dashboard
      </Link>
    </div>
  );
}
