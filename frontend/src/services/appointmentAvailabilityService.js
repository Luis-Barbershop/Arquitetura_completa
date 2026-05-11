import api from './api';

const WEEK_SHORT = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];
const AVAILABILITY_CACHE_TTL_MS = 60 * 1000;
const availabilityCache = new Map();
const inFlightAvailabilityRequests = new Map();

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

const buildAvailabilityCacheKey = ({ barberId, dateObj, durationMinutes }) => {
  const safeDuration = durationMinutes || 30;
  return `${String(barberId)}:${formatDateToApi(dateObj)}:${safeDuration}`;
};

const getCachedAvailabilitySlots = (cacheKey) => {
  const cachedEntry = availabilityCache.get(cacheKey);
  if (!cachedEntry) return null;

  if (cachedEntry.expiresAt <= Date.now()) {
    availabilityCache.delete(cacheKey);
    return null;
  }

  return [...cachedEntry.slots];
};

const cacheAvailabilitySlots = (cacheKey, slots) => {
  availabilityCache.set(cacheKey, {
    slots: [...slots],
    expiresAt: Date.now() + AVAILABILITY_CACHE_TTL_MS,
  });
};

export const clearAvailabilitySlotsCache = () => {
  availabilityCache.clear();
  inFlightAvailabilityRequests.clear();
};

export const fetchAvailabilitySlots = async ({
  barberId,
  dateObj,
  durationMinutes,
  forceRefresh = false,
}) => {
  if (!barberId || !dateObj) return [];

  const cacheKey = buildAvailabilityCacheKey({ barberId, dateObj, durationMinutes });

  if (!forceRefresh) {
    const cachedSlots = getCachedAvailabilitySlots(cacheKey);
    if (cachedSlots) {
      return cachedSlots;
    }

    const inFlightRequest = inFlightAvailabilityRequests.get(cacheKey);
    if (inFlightRequest) {
      const slots = await inFlightRequest;
      return [...slots];
    }
  }

  const requestPromise = api.get('/appointments/availability', {
    params: {
      barberId,
      date: formatDateToApi(dateObj),
      duration: durationMinutes || 30,
    },
  })
    .then((response) => {
      const slots = normalizeAvailabilitySlots(response.data);
      cacheAvailabilitySlots(cacheKey, slots);
      return slots;
    })
    .finally(() => {
      inFlightAvailabilityRequests.delete(cacheKey);
    });

  inFlightAvailabilityRequests.set(cacheKey, requestPromise);

  const slots = await requestPromise;
  return [...slots];
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
  minAdvanceHours = 0,
  forceRefresh = false,
}) => {
  const results = await Promise.allSettled(
    dateOptions.map((option) => fetchAvailabilitySlots({
      barberId,
      dateObj: option.date,
      durationMinutes,
      forceRefresh,
    })),
  );

  const now = new Date();

  return dateOptions.map((option, idx) => {
    let slots = results[idx].status === 'fulfilled' ? results[idx].value : [];

    if (minAdvanceHours > 0) {
      const dateStr = formatDateToApi(option.date);
      slots = slots.filter((slot) => {
        const slotDate = new Date(`${dateStr}T${slot}:00`);
        const diffMs = slotDate.getTime() - now.getTime();
        return diffMs >= minAdvanceHours * 60 * 60 * 1000;
      });
    }

    return {
      ...option,
      slots,
      isAvailable: slots.length > 0,
    };
  });
};
