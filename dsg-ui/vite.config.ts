import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Allow Caddy / custom DNS hostnames in local dev (Vite 6 host check)
    allowedHosts: ['dirsync.ringcentral.com', 'localhost', '127.0.0.1', 'dsg.local'],
    proxy: {
      '/dsg': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
