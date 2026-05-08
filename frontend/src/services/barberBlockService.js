import api from './api';

const BASE_URL = '/appointments/barber-blocks';

export const createBarberBlock = async ({ barberId, startTime, endTime, reason }) => {
  const response = await api.post(BASE_URL, {
    barberId,
    startTime,
    endTime,
    reason: reason?.trim() || null,
  });
  return response.data;
};

export const getBarberBlocks = async ({ barberId, date }) => {
  const response = await api.get(BASE_URL, {
    params: { barberId, date },
  });
  return response.data || [];
};

export const deleteBarberBlock = async (blockId) => {
  await api.delete(`${BASE_URL}/${blockId}`);
};
