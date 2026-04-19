import api from './api';

const WEEK_SHORT = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];

export const formatDateToApi = (dateObj) => {
  const y = dateObj.getFullYear();
  const m = String(dateObj.getMonth() + 1).padStart(2, '0');
  const d = String(dateObj.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

export const formatCompactDate = (dateObj) => (
  `${String(dateObj.getDate()).padStart(2, '0')}/${String(dateObj.getMonth() + 1).padStart(2, '0')}`
);

export const getRelativeDateLabel = (dateObj, idx) => {
  if (idx === 0) return 'Hoje';
  if (idx === 1) return 'Amanhã';
  return WEEK_SHORT[dateObj.getDay()];
};

export const buildDateWindow = (days = 14) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return Array.from({ length: days }, (_, i) => {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    return date;
  });
};

export const normalizeAvailabilitySlots = (availabilityResponse) => {
  const data = Array.isArray(availabilityResponse) ? availabilityResponse : [];

  return data
    .filter((slot) => slot.available)
    .map((slot) => {
      const raw = slot.startTime;
      if (!raw) return null;
      const part = raw.includes('T') ? raw.split('T')[1] : raw;
      return part.substring(0, 5);
    })
    .filter(Boolean);
};

export const fetchAvailabilitySlots = async ({
  barberId,
  dateObj,
  durationMinutes,
}) => {
  const response = await api.get('/appointments/availability', {
    params: {
      barberId,
      date: formatDateToApi(dateObj),
      duration: durationMinutes || 30,
    },
  });

  return normalizeAvailabilitySlots(response.data);
};

export const createDateOptionsBase = (days = 14) => {
  const window = buildDateWindow(days);

  return window.map((date, i) => ({
    key: formatDateToApi(date),
    date,
    label: getRelativeDateLabel(date, i),
    compact: formatCompactDate(date),
    slots: [],
    isAvailable: false,
  }));
};

export const hydrateDateOptionsWithAvailability = async ({
  barberId,
  durationMinutes,
  dateOptions,
}) => {
  const results = await Promise.allSettled(
    dateOptions.map((option) => fetchAvailabilitySlots({
      barberId,
      dateObj: option.date,
      durationMinutes,
    })),
  );

  return dateOptions.map((option, idx) => {
    const slots = results[idx].status === 'fulfilled' ? results[idx].value : [];
    return {
      ...option,
      slots,
      isAvailable: slots.length > 0,
    };
  });
};
