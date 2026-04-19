import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('react') || id.includes('scheduler')) {
            return 'vendor-react'
          }

          if (id.includes('react-router')) {
            return 'vendor-router'
          }

          if (id.includes('react-toastify')) {
            return 'vendor-toast'
          }

          if (id.includes('@phosphor-icons') || id.includes('react-icons')) {
            return 'vendor-icons'
          }

          return 'vendor'
        },
      },
    },
  },
})
