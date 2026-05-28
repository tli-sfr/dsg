/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        rc: {
          orange: '#ff8800',
          navy: '#1e2a3a',
        },
      },
    },
  },
  plugins: [],
};
