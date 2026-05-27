import '@testing-library/jest-dom/vitest';

import { afterEach, vi } from 'vitest';

import React from 'react';
globalThis.React = React;

// Stub heavy native modules used in the app (PDF renderer) to avoid file/URL issues in jsdom
vi.mock('@react-pdf/renderer', () => ({
  Image: () => null,
  Document: ({ children }) => children,
  Page: ({ children }) => children,
  StyleSheet: { create: () => ({}) },
  PDFDownloadLink: () => null,
}));

// Mock static asset imports used across components
vi.mock('/CortaAiLogo.png', () => ({ default: 'CORTAAI_LOGO' }));
vi.mock('/Icons/scissors_icon.png', () => ({ default: 'SCISSORS_ICON' }));

// Prevent SSE/EventSource usage during tests by stubbing the notification hook
vi.mock('../hooks/useNotificationStream', () => ({ useNotificationStream: () => {} }));

// Mock barbershop network calls to avoid real HTTP requests in page tests
vi.mock('../services/barbershopService', () => ({
  getShopServices: async () => [{ id: 's1', name: 'Corte Simples', price: 5000, durationMinutes: 30 }],
  getShopBarbers: async () => [{ id: 'b1', name: 'Barbeiro Teste' }],
  getBarbershopById: async () => ({ id: '123', name: 'Barbearia Teste' }),
  getShopActivities: async () => [],
  getMyFixedExpenses: async () => [],
}));

afterEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  vi.resetAllMocks();
});