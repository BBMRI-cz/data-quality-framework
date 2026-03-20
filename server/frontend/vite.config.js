import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

const PROXY_TARGET = "http://localhost:8082"

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src'),
        },
    },
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
