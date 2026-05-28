import { useState } from 'react';
import { api } from '../api/client';

export type DirectoryGroupOption = { id: string; name: string };

export function DirectoryGroupPickerDialog({
  accountId,
  open,
  onClose,
  onSelect,
}: {
  accountId: string;
  open: boolean;
  onClose: () => void;
  onSelect: (group: DirectoryGroupOption) => void;
}) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<DirectoryGroupOption[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  if (!open) {
    return null;
  }

  async function handleSearch() {
    const trimmed = query.trim();
    if (!trimmed) {
      setError('Enter at least one character to search.');
      return;
    }
    setSearching(true);
    setError(null);
    setSearched(true);
    try {
      const response = await api.searchDirectoryGroups(accountId, trimmed);
      setResults(response.groups);
    } catch (e) {
      setResults([]);
      setError(e instanceof Error ? e.message : 'Group search failed');
    } finally {
      setSearching(false);
    }
  }

  function handleSelect(group: DirectoryGroupOption) {
    onSelect(group);
    setQuery('');
    setResults([]);
    setSearched(false);
    setError(null);
    onClose();
  }

  function handleClose() {
    setQuery('');
    setResults([]);
    setSearched(false);
    setError(null);
    onClose();
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="directory-group-dialog-title"
        className="w-full max-w-lg rounded-xl border border-slate-200 bg-white shadow-xl"
      >
        <header className="border-b border-slate-100 px-6 py-4">
          <h2 id="directory-group-dialog-title" className="text-lg font-semibold text-rc-navy">
            Select directory group
          </h2>
          <p className="mt-1 text-sm text-slate-500">Search by group name, then select one result.</p>
        </header>

        <div className="space-y-4 px-6 py-5">
          <div className="flex gap-2">
            <input
              className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Search groups…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  void handleSearch();
                }
              }}
            />
            <button
              type="button"
              disabled={searching || !query.trim()}
              onClick={() => void handleSearch()}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              {searching ? 'Searching…' : 'Search'}
            </button>
          </div>

          {error && <p className="text-sm text-red-700">{error}</p>}

          <div className="max-h-64 overflow-y-auto rounded-lg border border-slate-200">
            {results.length > 0 ? (
              <ul className="divide-y divide-slate-100">
                {results.map((group) => (
                  <li key={group.id}>
                    <button
                      type="button"
                      className="flex w-full flex-col items-start px-4 py-3 text-left text-sm hover:bg-slate-50"
                      onClick={() => handleSelect(group)}
                    >
                      <span className="font-medium text-slate-900">{group.name}</span>
                      <span className="text-xs text-slate-500">{group.id}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="px-4 py-6 text-center text-sm text-slate-500">
                {searching
                  ? 'Searching…'
                  : searched
                    ? 'No groups matched your search.'
                    : 'Enter text and click Search to find groups.'}
              </p>
            )}
          </div>
        </div>

        <footer className="flex justify-end border-t border-slate-100 px-6 py-4">
          <button
            type="button"
            onClick={handleClose}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Cancel
          </button>
        </footer>
      </div>
    </div>
  );
}
