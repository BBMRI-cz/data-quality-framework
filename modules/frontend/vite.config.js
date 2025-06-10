import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
const PROXY_TARGET = "http://localhost:8081"

export default defineConfig({
  plugins: [
    vue()
  ],
  server: {
    port: 5173,
    proxy: {
      "^/api": {
        target: PROXY_TARGET,
        changeOrigin: true
      }
    }
  }
})
