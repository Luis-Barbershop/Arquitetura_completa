import api from "./api"

export const getAllBarbershops = async () => {
    try {
        const response = await api.get("/barbershops");
        return response.data;
    } catch (error) {
        console.error("Erro de API - API AllBarbershops", error);
        return [];
    }
}

export const getBarbershopById = async (id) => {
    try {
        const response = await api.get(`/barbershops/${id}`);
        return response.data;
    } catch (error) {
         console.error("Erro de API - API GetBarbershopById", error);
        return null;
    }
}

export const createBarbershop = async (shopData, imageFile) => {
    const formData = new FormData();

    // 1. LIMPEZA: Remove pontos, barras e traços, deixando apenas números
    const cleanCnpj = shopData.cnpj.replace(/\D/g, ''); 

    // 2. Monta o objeto JSON com o CNPJ limpo
    const shopObject = {
        name: shopData.name,
        cnpj: cleanCnpj, // Agora envia "48719131000150" (14 dígitos)
        address: shopData.address
    };

    // 3. Cria o Blob JSON
    const shopJsonString = JSON.stringify(shopObject);
    const jsonBlob = new Blob([shopJsonString], {
        type: 'application/json'
    });

    // 4. Adiciona ao FormData
    formData.append('shop', jsonBlob);

    // 5. Adiciona imagem se existir
    if (imageFile) {
        formData.append('file', imageFile);
    }

    const response = await api.post('/barbershops/register-my-shop', formData);
    
    return response.data;
};

export const updateMyBarbershop = async (shopData) => {
    const response = await api.put('/barbershops/my-shop', shopData);
    return response.data;
};

export const uploadMyBarbershopLogo = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/barbershops/my-shop/upload-logo', formData);
    return response.data;
};

export const uploadMyBarbershopBanner = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/barbershops/my-shop/upload-banner', formData);
    return response.data;
};

// Busca os barbeiros de uma loja específica (Público)
export const getShopBarbers = async (shopId) => {
    if (!shopId) return [];

    const normalizeBarbersPayload = (payload) => {
        if (Array.isArray(payload)) return payload;
        if (Array.isArray(payload?.content)) return payload.content;
        if (Array.isArray(payload?.data)) return payload.data;
        if (Array.isArray(payload?.barbers)) return payload.barbers;
        return [];
    };

    try {
        const response = await api.get(`/barbers/barbershop/${shopId}`);
        const parsed = normalizeBarbersPayload(response.data);
        if (parsed.length > 0) return parsed;
    } catch (error) {
        console.warn("Falha na rota principal de barbeiros:", error?.response?.status || error?.message);
    }

    try {
        const fallbackResponse = await api.get(`/barbershops/${shopId}/barbers`);
        return normalizeBarbersPayload(fallbackResponse.data);
    } catch (fallbackError) {
        console.error("Erro ao buscar barbeiros da loja:", fallbackError);
        return [];
    }
};

export const getShopServices = async (shopId) => {
    try {
        const response = await api.get(`/barbershops/${shopId}/activities`);
        return response.data; // Retorna lista de ActivityDTO
    } catch (error) {
        console.error("Erro ao buscar serviços da loja:", error);
        return [];
    }
};

export const getMyFavoriteBarbershopsIds = async () => {
    try {
        const response = await api.get('/customers/me/favorites');
        return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
        console.error('Erro ao buscar favoritas:', error);
        return [];
    }
};

export const addFavoriteBarbershop = async (shopId) => {
    await api.post(`/customers/me/favorites/${shopId}`);
};

export const removeFavoriteBarbershop = async (shopId) => {
    await api.delete(`/customers/me/favorites/${shopId}`);
};

export const createBarbershopReview = async (shopId, reviewData) => {
    await api.post(`/barbershops/${shopId}/reviews`, reviewData);
};

export const hasReviewedBarbershop = async (shopId) => {
    const response = await api.get(`/barbershops/${shopId}/reviews/me`);
    return Boolean(response.data?.reviewed);
};


export const getMyServices = async () => {
    // Busca os dados do barbeiro autenticado e depois usa o barbershopId
    // para listar os serviços da loja.
    try {
        const barberResponse = await api.get('/auth/me');
        const shopId = barberResponse.data?.barbershopId;
        
        if(!shopId) return [];

        // Usa a rota pública de listar serviços, já que serve para o dono também
        const response = await api.get(`/barbershops/${shopId}/activities`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar meus serviços:", error);
        return [];
    }
};

// Criar novo serviço
export const createService = async (serviceData) => {
    // serviceData: { activityName, price, durationMinutes }
    const response = await api.post('/barbershops/my-shop/activities', serviceData);
    return response.data;
};

// Deletar serviço
export const deleteService = async (serviceId) => {
    await api.delete(`/barbershops/my-shop/activities/${serviceId}`);
};

export const getMyAssignedActivities = async () => {
    const response = await api.get('/barbers/me/my-activities');
    const payload = response.data;
    if (!Array.isArray(payload)) return [];

    return payload
        .map((item) => {
            if (typeof item === 'string') return item;
            if (item && typeof item === 'object' && item.id) return String(item.id);
            return null;
        })
        .filter(Boolean);
};

// Vincula atividades ao perfil do Barbeiro
export const assignActivities = async (activityIds) => {
    // O DTO espera { activityIds: [uuid, uuid] }
    const response = await api.post('/barbers/me/assign-activities', { activityIds });
    return response.data;
};

// ── Fluxo de convite (Owner → Barbeiro) ────────────────────────────────────

/** Owner convida barbeiro pelo CPF */
export const inviteBarberByCpf = async (cpf) => {
    const response = await api.post('/barbershops/my-shop/invite-barber', { cpf });
    return response.data;
};

/** Barbeiro lista convites pendentes recebidos */
export const getMyInvites = async () => {
    const normalizeInvitesPayload = (payload) => {
        if (Array.isArray(payload)) return payload;
        if (Array.isArray(payload?.content)) return payload.content;
        if (Array.isArray(payload?.data)) return payload.data;
        return [];
    };

    try {
        const response = await api.get('/barbershops/my-invites');
        return normalizeInvitesPayload(response.data);
    } catch (error) {
        console.warn('Falha ao buscar convites pendentes:', error?.response?.status || error?.message);
        return [];
    }
};

/** Barbeiro aceita convite */
export const acceptInvite = async (requestId) => {
    const response = await api.post(`/barbershops/accept-invite/${requestId}`);
    return response.data;
};

/** Barbeiro recusa convite */
export const rejectInvite = async (requestId) => {
    const response = await api.post(`/barbershops/reject-invite/${requestId}`);
    return response.data;
};

/** Barbeiro colaborador sai da barbearia atual */
export const leaveShop = async () => {
    const response = await api.post('/barbershops/leave-shop');
    return response.data;
};

// ── Agenda semanal (blocos de horário por dia) ─────────────────────────────

/** Busca a agenda semanal do barbeiro autenticado */
export const getMyWorkSchedule = async () => {
    try {
        const response = await api.get('/barbers/me/work-schedule');
        return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
        console.error('Erro ao buscar agenda semanal:', error);
        return [];
    }
};

/**
 * Salva (substitui) toda a agenda semanal do barbeiro.
 * @param {Array} schedule - Array de { dayOfWeek: 'MONDAY', blocks: [{startTime:'09:00', endTime:'12:00'}] }
 */
export const saveMyWorkSchedule = async (data) => {
    const response = await api.put('/barbers/me/work-schedule', data);
    return response.data;
};

export const getBarbershops = ({ lat, lng, radiusKm = 10 } = {}) => {
    const params = lat != null ? { lat, lng, radiusKm } : {};
    return api.get('/barbershops', { params }).then(r => r.data);
};

export const geocodeAddress = async (address) => {
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address)}&format=json&limit=1`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'pt-BR' } });
    const data = await res.json();
    if (!data.length) return null;
    return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) };
};
