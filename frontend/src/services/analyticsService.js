import api from './api';

export const getBarberPerformance = (barbershopId) =>
    api.get('/payments/analytics/barber-performance', { params: { barbershopId } }).then(r => r.data);

export const getMyShopBarberPerformance = (barbershopId, params = {}) =>
    api.get('/payments/my-shop/barber-performance', { params: { barbershopId, ...params } }).then(r => r.data);

export const getFinancialOverview = (barbershopId, params = {}) =>
    api.get('/payments/my-shop/overview', { params: { barbershopId, ...params } }).then(r => r.data);

export const getFinancialSeries = (barbershopId, params = {}) =>
    api.get('/payments/my-shop/series', { params: { barbershopId, ...params } }).then(r => r.data);

export const getBarberFinancialSummary = (barbershopId, params = {}) =>
    api.get('/payments/my-shop/barber-summary', { params: { barbershopId, ...params } }).then(r => r.data);

export const getStockHealthAlert = (barbershopId) =>
    api.get('/products/analytics/stock-health', { params: { barbershopId } }).then(r => r.data);

export const getAgendaThermometer = (barbershopId) =>
    api.get('/appointments/analytics/agenda-thermometer', { params: { barbershopId } }).then(r => r.data);

export const getBarberSkillMatrix = (barbershopId) =>
    api.get('/appointments/analytics/barber-skill-matrix', { params: { barbershopId } }).then(r => r.data);

export const getCustomerAcquisition = () =>
    api.get('/users/analytics/customer-acquisition').then(r => r.data);

export const getCustomerRetention = () =>
    api.get('/users/analytics/customer-retention').then(r => r.data);
