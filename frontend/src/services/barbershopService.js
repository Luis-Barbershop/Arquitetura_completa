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

// Busca os barbeiros de uma loja específica (Público)
export const getShopBarbers = async (shopId) => {
    if (!shopId) return [];

    const normalizeBarbersPayload = (payload) => {
        if (Array.isArray(payload)) return payload;
        if (Array.isArray(payload?.content)) return payload.content;
        if (Array.isArray(payload?.data)) return payload.data;
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
    return response.data; // Lista de ActivityDTO
};

// Vincula atividades ao perfil do Barbeiro
export const assignActivities = async (activityIds) => {
    // O DTO espera { activityIds: [uuid, uuid] }
    const response = await api.post('/barbers/me/assign-activities', { activityIds });
    return response.data;
};