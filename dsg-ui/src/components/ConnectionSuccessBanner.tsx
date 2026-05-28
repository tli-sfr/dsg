import { useEffect } from 'react';

export function ConnectionSuccessBanner({
  firstName,
  lastName,
  onDismiss,
  durationMs = 3000,
}: {
  firstName: string | null;
  lastName: string | null;
  onDismiss: () => void;
  durationMs?: number;
}) {
  useEffect(() => {
    const timer = window.setTimeout(onDismiss, durationMs);
    return () => window.clearTimeout(timer);
  }, [durationMs, onDismiss]);

  const displayName = [firstName, lastName].filter(Boolean).join(' ').trim();

  return (
    <div
      role="status"
      className="fixed left-1/2 top-4 z-[100] -translate-x-1/2 rounded-lg bg-green-600 px-6 py-3 text-center text-sm font-medium text-white shadow-lg"
    >
      {displayName
        ? `Successfully connected with ${displayName}`
        : 'Successfully connected'}
    </div>
  );
}
