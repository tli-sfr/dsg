import { AddContactMd, TrashMd } from '@ringcentral/spring-icon';
import {
  Button,
  Option,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@ringcentral/spring-ui';
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
        <Button
          variant="outlined"
          color="primary"
          size="small"
          disabled={saving || rows.length === 0}
          onClick={save}
        >
          {saving ? 'Saving…' : 'Save mappings'}
        </Button>
      }
    >
      <p className="mb-4 typography-label text-neutral-b3">
        Attribute mappings define how attributes are synchronized between {providerLabel} and
        RingCentral.
        {config && !config.accountConfigured && (
          <span className="ml-1 text-neutral-b4">(Showing defaults — save to customize.)</span>
        )}
      </p>

      <Table>
        <TableHead>
          <TableRow>
            <TableCell>{providerLabel} attribute</TableCell>
            <TableCell>RingCentral attribute</TableCell>
            <TableCell>Remove</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, index) => (
            <TableRow key={`${row.directoryAttributePath}-${row.rcAttribute}-${index}`}>
              <TableCell>
                <span
                  className="block max-w-xs truncate font-mono typography-descriptorMini"
                  title={row.directoryAttributePath}
                >
                  {row.directoryAttributePath}
                </span>
              </TableCell>
              <TableCell>{row.rcAttribute}</TableCell>
              <TableCell>
                <Button
                  variant="text"
                  color="primary"
                  size="small"
                  startIcon={TrashMd}
                  onClick={() => removeRow(index)}
                >
                  Delete
                </Button>
              </TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={3}>
                <span className="typography-label text-neutral-b3">
                  No mappings configured. Add a mapping below.
                </span>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      {!showAdd ? (
        <Button
          variant="text"
          color="primary"
          size="small"
          startIcon={AddContactMd}
          className="mt-4"
          onClick={() => setShowAdd(true)}
        >
          Add new mapping
        </Button>
      ) : (
        <div className="mt-4 flex flex-wrap items-end gap-3 rounded-sui-md border border-neutral-b4 bg-neutral-b5 p-4">
          <Select
            label="Directory attribute"
            placeholder="Select…"
            value={addDirPath || null}
            onChange={(e) => setAddDirPath(e.target.value)}
            className="w-56"
          >
            {addableDirectory.map((d) => (
              <Option key={d.attributePath} value={d.attributePath}>
                {d.attributePath} ({d.attributeName})
              </Option>
            ))}
          </Select>
          <Select
            label="RingCentral attribute"
            placeholder="Select…"
            value={addRc || null}
            onChange={(e) => setAddRc(e.target.value)}
            className="w-48"
          >
            {addableRc.map((r) => (
              <Option key={r.attributeName} value={r.attributeName}>
                {r.displayName} ({r.attributeName})
              </Option>
            ))}
          </Select>
          <Button variant="contained" disabled={!addDirPath || !addRc} onClick={confirmAdd}>
            Add
          </Button>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => {
              setShowAdd(false);
              setAddDirPath('');
              setAddRc('');
            }}
          >
            Cancel
          </Button>
        </div>
      )}
    </Card>
  );
}
