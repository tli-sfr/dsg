import 'react';

declare module 'react' {
  interface HTMLAttributes<T> extends React.HTMLAttributes<T> {
    onPointerEnterCapture?: (e: React.PointerEvent<T>) => void;
    onPointerLeaveCapture?: (e: React.PointerEvent<T>) => void;
    placeholder?: string;
  }

  interface RefAttributes<T> extends React.RefAttributes<T> {
    onPointerEnterCapture?: (e: React.PointerEvent<T>) => void;
    onPointerLeaveCapture?: (e: React.PointerEvent<T>) => void;
    placeholder?: string;
  }
}
