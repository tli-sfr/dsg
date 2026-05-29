import { createRequire } from 'module';

const require = createRequire(import.meta.url);

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
    'node_modules/@ringcentral/spring-ui/**/*.js',
  ],
  plugins: [
    require('@ringcentral/spring-theme/tailwind')({
      override: false,
    }),
  ],
};
