import { useSearchParams } from 'react-router-dom';

export function AccountBar() {
  const [params, setParams] = useSearchParams();
  const accountId =
    params.get('accountId') ??
    import.meta.env.VITE_DEFAULT_ACCOUNT_ID ??
    'demo-acct';

  return (
    <div className="flex items-center gap-3 border-b border-slate-200 bg-white px-6 py-3">
      <span className="text-sm font-medium text-slate-600">Account ID</span>
      <input
        className="rounded border border-slate-300 px-2 py-1 text-sm"
        value={accountId}
        onChange={(e) => setParams({ accountId: e.target.value })}
      />
    </div>
  );
}

export function useAccountId(): string {
  const [params] = useSearchParams();
  return params.get('accountId') ?? import.meta.env.VITE_DEFAULT_ACCOUNT_ID ?? 'demo-acct';
}
