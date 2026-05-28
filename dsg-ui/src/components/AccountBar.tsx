import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';

export function AccountBar() {
  const [params, setParams] = useSearchParams();
  const urlAccountId =
    params.get('accountId') ??
    import.meta.env.VITE_DEFAULT_ACCOUNT_ID ??
    'demo-acct';
  const [displayAccountId, setDisplayAccountId] = useState(urlAccountId);
  const [extensionLabel, setExtensionLabel] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setDisplayAccountId(urlAccountId);
    let cancelled = false;

    async function resolveRcAccount() {
      setLoading(true);
      try {
        const status = await api.getRcOAuthStatus(urlAccountId);
        if (cancelled || !status.connected) {
          return;
        }
        const session = await api.getRcOAuthSession(urlAccountId);
        if (cancelled) return;

        setDisplayAccountId(session.rcAccountId);
        if (session.extensionName) {
          setExtensionLabel(`${session.extensionName} (ext. ${session.extensionNumber ?? '—'})`);
        } else if (session.extensionNumber) {
          setExtensionLabel(`ext. ${session.extensionNumber}`);
        }

        if (session.rcAccountId !== urlAccountId) {
          setParams({ accountId: session.rcAccountId }, { replace: true });
        }
      } catch {
        /* keep URL account id */
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    resolveRcAccount();
    return () => {
      cancelled = true;
    };
  }, [urlAccountId, setParams]);

  return (
    <div className="flex items-center gap-3 border-b border-slate-200 bg-white px-6 py-3">
      <span className="text-sm font-medium text-slate-600">Account ID</span>
      <input
        className="rounded border border-slate-300 bg-slate-50 px-2 py-1 text-sm"
        value={loading ? 'Loading…' : displayAccountId}
        readOnly
        aria-label="RingCentral account ID"
      />
      {extensionLabel && (
        <span className="text-sm text-slate-500">{extensionLabel}</span>
      )}
    </div>
  );
}

export function useAccountId(): string {
  const [params] = useSearchParams();
  return params.get('accountId') ?? import.meta.env.VITE_DEFAULT_ACCOUNT_ID ?? 'demo-acct';
}
