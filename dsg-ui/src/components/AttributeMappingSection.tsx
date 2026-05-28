import { Plus, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import type { AttributeMappingConfig, AttributeMappingRow } from '../api/types';
import { Card } from './Card';

type MappingRow = {
  directoryAttributePath: string;
  directoryAttributeName: string;
  rcAttribute: string;
};

export function AttributeMappingSection({
  accountId,
  directoryType,
  onMessage,
  onError,
}: {
  accountId: string;
  directoryType: string | undefined;
  onMessage: (msg: string) => void;
  onError: (msg: string) => void;
}) {
  const [config, setConfig] = useState<AttributeMappingConfig | null>(null);
  const [rows, setRows] = useState<MappingRow[]>([]);
  const [showAdd, setShowAdd] = useState(false);
  const [addDirPath, setAddDirPath] = useState('');
  const [addRc, setAddRc] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!directoryType) return;
    try {
      const data = await api.getAttributeMapping(accountId);
      setConfig(data);
      setRows(
        data.mappings.map((m) => ({
          directoryAttributePath: m.directoryAttributePath,
          directoryAttributeName: m.directoryAttributeName,
          rcAttribute: m.rcAttribute,
        })),
      );
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load attribute mappings');
    }
  }, [accountId, directoryType, onError]);

  useEffect(() => {
    load();
  }, [load]);

  const usedKeys = useMemo(
    () => new Set(rows.map((r) => `${r.directoryAttributePath}\0${r.rcAttribute}`)),
    [rows],
  );

  const addableDirectory = useMemo(() => {
    if (!config) return [];
    return config.directoryAttributes.filter(
      (d) => !rows.some((r) => r.directoryAttributePath === d.attributePath),
    );
  }, [config, rows]);

  const addableRc = useMemo(() => {
    if (!config) return [];
    return config.rcAttributes.filter((r) => !rows.some((row) => row.rcAttribute === r.attributeName));
  }, [config, rows]);

  function removeRow(index: number) {
    setRows((prev) => prev.filter((_, i) => i !== index));
  }

  function confirmAdd() {
    if (!config || !addDirPath || !addRc) return;
    const dir = config.directoryAttributes.find((d) => d.attributePath === addDirPath);
    const rc = config.rcAttributes.find((r) => r.attributeName === addRc);
    if (!dir || !rc) return;
    const key = `${dir.attributePath}\0${rc.attributeName}`;
    if (usedKeys.has(key)) return;
    setRows((prev) => [
      ...prev,
      {
        directoryAttributePath: dir.attributePath,
        directoryAttributeName: dir.attributeName,
        rcAttribute: rc.attributeName,
      },
    ]);
    setShowAdd(false);
    setAddDirPath('');
    setAddRc('');
  }

  async function save() {
    setSaving(true);
    onError('');
    try {
      const basicMappings: AttributeMappingRow[] = rows.map((r) => ({
        syncDirection: 'DIR_TO_RC',
        directoryAttribute: r.directoryAttributePath,
        rcAttribute: r.rcAttribute,
      }));
      await api.saveAttributeMapping(accountId, {
        basicMappings,
        customMappings: [],
      });
      onMessage('Attribute mappings saved');
      await load();
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to save attribute mappings');
    } finally {
      setSaving(false);
    }
  }

  if (!directoryType) {
    return null;
  }

  const providerLabel =
    directoryType === 'Azure'
      ? 'Azure Active Directory'
      : directoryType === 'Google'
        ? 'Google Workspace'
        : directoryType;

  return (
    <Card
      title="Attribute mappings"
      action={
        <button
          type="button"
          disabled={saving || rows.length === 0}
          onClick={save}
          className="rounded bg-rc-navy px-3 py-1 text-xs font-medium text-white disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save mappings'}
        </button>
      }
    >
      <p className="mb-4 text-sm text-slate-500">
        Attribute mappings define how attributes are synchronized between {providerLabel} and
        RingCentral.
        {config && !config.accountConfigured && (
          <span className="ml-1 text-slate-400">(Showing defaults — save to customize.)</span>
        )}
      </p>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b text-slate-500">
            <th className="py-2 pr-4">{providerLabel} attribute</th>
            <th className="py-2 pr-4">RingCentral attribute</th>
            <th className="py-2 w-20">Remove</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={`${row.directoryAttributePath}-${row.rcAttribute}-${index}`} className="border-b border-slate-50">
              <td className="max-w-xs truncate py-2 pr-4 font-mono text-xs" title={row.directoryAttributePath}>
                {row.directoryAttributePath}
              </td>
              <td className="py-2 pr-4">{row.rcAttribute}</td>
              <td className="py-2">
                <button
                  type="button"
                  onClick={() => removeRow(index)}
                  className="inline-flex items-center gap-1 text-xs text-rc-orange hover:underline"
                  aria-label="Remove mapping"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  Delete
                </button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr>
              <td colSpan={3} className="py-4 text-slate-500">
                No mappings configured. Add a mapping below.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {!showAdd ? (
        <button
          type="button"
          onClick={() => setShowAdd(true)}
          className="mt-4 inline-flex items-center gap-1 text-sm text-rc-orange hover:underline"
        >
          <Plus className="h-4 w-4" />
          Add new mapping
        </button>
      ) : (
        <div className="mt-4 flex flex-wrap items-end gap-3 rounded border border-slate-200 bg-slate-50 p-4">
          <label className="text-sm">
            Directory attribute
            <select
              className="mt-1 block w-56 rounded border border-slate-300 px-2 py-1 text-sm"
              value={addDirPath}
              onChange={(e) => setAddDirPath(e.target.value)}
            >
              <option value="">Select…</option>
              {addableDirectory.map((d) => (
                <option key={d.attributePath} value={d.attributePath}>
                  {d.attributePath} ({d.attributeName})
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            RingCentral attribute
            <select
              className="mt-1 block w-48 rounded border border-slate-300 px-2 py-1 text-sm"
              value={addRc}
              onChange={(e) => setAddRc(e.target.value)}
            >
              <option value="">Select…</option>
              {addableRc.map((r) => (
                <option key={r.attributeName} value={r.attributeName}>
                  {r.displayName} ({r.attributeName})
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            onClick={confirmAdd}
            disabled={!addDirPath || !addRc}
            className="rounded bg-rc-orange px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50"
          >
            Add
          </button>
          <button
            type="button"
            onClick={() => {
              setShowAdd(false);
              setAddDirPath('');
              setAddRc('');
            }}
            className="rounded border border-slate-300 px-3 py-1.5 text-xs"
          >
            Cancel
          </button>
        </div>
      )}
    </Card>
  );
}
