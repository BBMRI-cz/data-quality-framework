import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const PROXY_TARGET = "http://localhost:8081"

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '^/api': {
        target: PROXY_TARGET,
        changeOrigin: true,
      },
    },
  },
  base: '/', // Ensures assets are served from the root in production
  build: {
    outDir: 'dist', // Default output directory, matches Dockerfile
    assetsDir: 'assets', // Default, ensures assets are organized
  },
})