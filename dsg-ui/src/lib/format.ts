import type { SelectionCriterion } from '../api/types';

export function formatSelectionExpression(expr: Record<string, unknown> | null): string {
  if (!expr || expr.match === 'ALL' && (!expr.criteria || (expr.criteria as unknown[]).length === 0)) {
    return 'All users';
  }
  const criteria = (expr.criteria as SelectionCriterion[]) ?? [];
  return criteria
    .map((c) => `${c.attribute} == '${c.value}'`)
    .join(' AND ');
}

export function formatInstant(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString();
}
