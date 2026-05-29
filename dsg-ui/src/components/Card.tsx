import type { ReactNode } from 'react';
import { Block } from '@ringcentral/spring-ui';

export function Card({
  title,
  children,
  action,
}: {
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <Block>
      <div className="mb-3 flex w-full items-center gap-3 border-b border-neutral-b4 pb-3">
        <span className="min-w-0 flex-1 typography-subtitleMiniSemiBold text-neutral-b1">{title}</span>
        {action ? <div className="ml-auto shrink-0">{action}</div> : null}
      </div>
      {children}
    </Block>
  );
}
