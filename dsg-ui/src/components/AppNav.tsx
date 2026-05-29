import { NavLink, useSearchParams } from 'react-router-dom';

type NavTab = { to: string; label: string; end?: boolean };

const tabs: NavTab[] = [
  { to: '/directory-integration', label: 'Directory Integration', end: true },
  { to: '/directory-integration/configuration', label: 'Directory Configuration' },
  { to: '/directory-integration/sync-history', label: 'Sync History' },
];

export function AppNav() {
  const [params] = useSearchParams();
  const accountQuery = params.get('accountId')
    ? `?accountId=${encodeURIComponent(params.get('accountId')!)}`
    : '';

  return (
    <nav className="border-b border-neutral-b4 bg-neutral-base px-6">
      <div className="flex gap-6">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={`${tab.to}${accountQuery}`}
            end={tab.end ?? false}
            className={({ isActive }) =>
              `border-b-2 py-3 typography-labelSemiBold ${
                isActive
                  ? 'border-primary-b text-neutral-b1'
                  : 'border-transparent text-neutral-b3 hover:text-neutral-b1'
              }`
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
