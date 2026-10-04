import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // sockjs-client (dùng cho WebSocket cảnh báo Triage) tham chiếu biến `global` của Node,
  // trình duyệt không có nên trang trắng với "global is not defined".
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    rolldownOptions: {
      transform: {
        define: { global: 'globalThis' },
      },
    },
  },
})
