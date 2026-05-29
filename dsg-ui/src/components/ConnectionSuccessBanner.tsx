import { Snackbar, SnackbarContent } from '@ringcentral/spring-ui';
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
    <Snackbar open onClose={onDismiss} autoHideDuration={durationMs}>
      <SnackbarContent severity="success">
        {displayName
          ? `Successfully connected with ${displayName}`
          : 'Successfully connected'}
      </SnackbarContent>
    </Snackbar>
  );
}
