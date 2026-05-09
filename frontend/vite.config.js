import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@reduxjs/toolkit': resolve(process.cwd(), 'node_modules/@reduxjs/toolkit/dist/cjs/index.js'),
    },
  },
})
