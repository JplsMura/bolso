/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Fora do Docker a API está em localhost; no serviço web-dev do compose, em http://api:8080.
const api = process.env.VITE_API_PROXY ?? 'http://localhost:8080'
// Pasta montada do Windows num contêiner Linux não avisa quando um arquivo muda: o web-dev liga a varredura.
const varredura = process.env.VITE_POLLING === 'true'

export default defineConfig({
  // o plugin do router precisa vir antes do react
  plugins: [tanstackRouter({ target: 'react', autoCodeSplitting: true }), react(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: { '/api': api, '/actuator': api, '/v3': api },
    ...(varredura ? { watch: { usePolling: true, interval: 300 } } : {}),
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
