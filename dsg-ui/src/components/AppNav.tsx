import { NavLink, useSearchParams } from 'react-router-dom';

type NavTab = { to: string; label: string; end?: boolean };

const tabs: NavTab[] = [
  { to: '/directory-integration', label: 'Directory Integration', end: true },
  { to: '/directory-integration/configuration', label: 'Directory Configuration' },
];

export function AppNav() {
  const [params] = useSearchParams();
  const accountQuery = params.get('accountId')
    ? `?accountId=${encodeURIComponent(params.get('accountId')!)}`
    : '';

  return (
    <nav className="border-b border-slate-200 bg-white px-6">
      <div className="flex gap-6">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={`${tab.to}${accountQuery}`}
            end={tab.end ?? false}
            className={({ isActive }) =>
              `border-b-2 py-3 text-sm font-medium ${
                isActive
                  ? 'border-rc-orange text-rc-navy'
                  : 'border-transparent text-slate-500 hover:text-rc-navy'
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
