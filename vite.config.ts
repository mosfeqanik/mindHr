import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev: /api is proxied to the Spring Boot API, so the browser stays same-origin (the backend has no CORS config).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080', changeOrigin: true },
    },
  },
})
