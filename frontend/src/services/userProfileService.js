import api from './api';

const toFormData = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
};

export const getMyProfile = async () => {
  const response = await api.get('/auth/me');
  return response.data;
};

export const updateCustomerProfile = async (payload) => {
  const response = await api.put('/customers/me', payload);
  return response.data;
};

export const uploadCustomerProfilePhoto = async (file) => {
  const response = await api.post('/customers/me/upload-photo', toFormData(file));
  return response.data;
};

export const uploadBarberProfilePhoto = async (file) => {
  const response = await api.post('/barbers/me/upload-photo', toFormData(file));
  return response.data;
};
