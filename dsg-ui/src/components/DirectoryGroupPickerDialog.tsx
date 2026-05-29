import { useState } from 'react';
import {
  Alert,
  Block,
  BlockHeader,
  Button,
  List,
  ListItem,
  ListItemText,
  Modal,
  TextField,
} from '@ringcentral/spring-ui';
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
    <Modal open={open} onClose={handleClose} className="flex items-center justify-center p-4">
      <Block className="w-full max-w-lg">
        <BlockHeader divider>
          <div>
            <div id="directory-group-dialog-title" className="typography-subtitleMiniSemiBold">
              Select directory group
            </div>
            <p className="typography-label text-neutral-b3">
              Search by group name, then select one result.
            </p>
          </div>
        </BlockHeader>

        <div className="space-y-4">
          <div className="flex gap-2">
            <TextField
              className="flex-1"
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
            <Button
              variant="contained"
              disabled={searching || !query.trim()}
              onClick={() => void handleSearch()}
            >
              {searching ? 'Searching…' : 'Search'}
            </Button>
          </div>

          {error && <Alert severity="error">{error}</Alert>}

          <div className="max-h-64 overflow-y-auto rounded-sui-sm border border-neutral-b4">
            {results.length > 0 ? (
              <List>
                {results.map((group) => (
                  <ListItem key={group.id} clickable onClick={() => handleSelect(group)}>
                    <ListItemText primary={group.name} secondary={group.id} />
                  </ListItem>
                ))}
              </List>
            ) : (
              <p className="px-4 py-6 text-center typography-label text-neutral-b3">
                {searching
                  ? 'Searching…'
                  : searched
                    ? 'No groups matched your search.'
                    : 'Enter text and click Search to find groups.'}
              </p>
            )}
          </div>

          <div className="flex justify-end">
            <Button variant="outlined" color="secondary" onClick={handleClose}>
              Cancel
            </Button>
          </div>
        </div>
      </Block>
    </Modal>
  );
}
