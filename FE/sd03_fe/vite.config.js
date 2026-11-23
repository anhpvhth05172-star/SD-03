import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const isAdmin = mode === 'admin'
  const defaultPort = isAdmin ? 5174 : 5173

  return {
    plugins: [
      vue(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: defaultPort,
      strictPort: false,
      // FE gọi /api/* cùng origin với trang (localhost hay IP LAN đều được),
      // Vite tự chuyển tiếp về BE chạy trên localhost:8080.
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // baseAPI (trang giảm giá...) gọi BE ở root /admin/* không có prefix /api
        '/admin': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
