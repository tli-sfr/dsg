import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { suiLight, ThemeProvider } from '@ringcentral/spring-theme';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={suiLight}>
      <div className="spring-ui-jupiter min-h-screen bg-neutral-b5">
        <App />
      </div>
    </ThemeProvider>
  </StrictMode>,
);
