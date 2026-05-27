import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setupTests.js',
    clearMocks: true,
    restoreMocks: true,
    coverage: {
      reporter: ['text', 'lcov', 'html'],
      reportsDirectory: 'coverage',
      include: ['src/**/*.{js,ts,jsx,tsx}'],
      exclude: ['**/*.test.*', 'node_modules', 'dist'],
    },
  },
});