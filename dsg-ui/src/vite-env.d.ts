/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEFAULT_ACCOUNT_ID: string;
  readonly VITE_API_BASE: string;
  readonly VITE_RC_CLIENT_ID: string;
  readonly VITE_RC_SERVER_URL: string;
  readonly VITE_RC_REDIRECT_URI: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
