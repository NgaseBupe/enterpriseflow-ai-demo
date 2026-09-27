import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    // Same-origin in development too: the browser talks to Vite, Vite forwards /api to the backend.
    proxy: {
      '/api': process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
