import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/Apk/',
  plugins: [react()],
  build: {
    sourcemap: true,
  },
});
