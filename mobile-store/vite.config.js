import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom', // This tells Vitest to simulate a browser environment
    globals: true, // This allows us to use describe, it, expect without importing them everywhere
  },
})