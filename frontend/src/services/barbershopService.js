import api from "./api"

// GET /api/barbershops
export const getAllBarbershops = async () => {
    try {
        const response = await api.get("/barbershops");
        return response.data;
    } catch (error) {
        console.error("Erro de API - getAllBarbershops:", error);
        return [];
    }
};

// GET /api/barbershops/{id}
export const getBarbershopById = async (id) => {
    try {
        const response = await api.get(`/barbershops/${id}`);
        return response.data;
    } catch (error) {
        console.error("Erro de API - getBarbershopById:", error);
        return null;
    }
};

// POST /api/barbershops/register-my-shop
export const createBarbershop = async (shopData, imageFile) => {
    const formData = new FormData();

    // Limpeza: Remove pontos, barras e traços, deixando apenas números
    const cleanCnpj = shopData.cnpj.replace(/\D/g, '');

    const shopObject = {
        name: shopData.name,
        cnpj: cleanCnpj,
        address: shopData.address
    };

    const shopJsonString = JSON.stringify(shopObject);
    const jsonBlob = new Blob([shopJsonString], { type: 'application/json' });
    formData.append('shop', jsonBlob);

    if (imageFile) {
        formData.append('file', imageFile);
    }

    const response = await api.post('/barbershops/register-my-shop', formData);
    return response.data;
};

// Busca os barbeiros de uma barbearia
// GET /api/barbers/barbershop/{barbershopId} (rota no user-service, gateway roteia)
export const getShopBarbers = async (shopId) => {
    try {
        const response = await api.get(`/barbers/barbershop/${shopId}`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar barbeiros da loja:", error);
        return [];
    }
};

// GET /api/barbershops/{shopId}/activities
export const getShopServices = async (shopId) => {
    try {
        const response = await api.get(`/barbershops/${shopId}/activities`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar serviços da loja:", error);
        return [];
    }
};

// Busca serviços da barbearia do barbeiro logado
// Usa /api/auth/me para descobrir barbershopId, depois lista activities
export const getMyServices = async () => {
    try {
        const meResponse = await api.get('/auth/me');
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        // O barbershopId pode estar no user armazenado ou precisar de outra chamada
        // Busca pelo barber endpoint com o userId
        const barberResponse = await api.get(`/barbers/${meResponse.data.id}`);
        const shopId = barberResponse.data.barbershopId;

        if (!shopId) return [];

        const response = await api.get(`/barbershops/${shopId}/activities`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar meus serviços:", error);
        return [];
    }
};

// POST /api/barbershops/my-shop/activities
export const createService = async (serviceData) => {
    const response = await api.post('/barbershops/my-shop/activities', serviceData);
    return response.data;
};

// DELETE /api/barbershops/my-shop/activities/{id}
export const deleteService = async (serviceId) => {
    await api.delete(`/barbershops/my-shop/activities/${serviceId}`);
};

// Busca atividades vinculadas ao barbeiro logado
// Nota: Esse endpoint precisa ser criado no backend (GET /api/barbers/{id}/activities)
// Por enquanto, usa workaround: busca todas activities da shop e filtra no frontend
export const getMyAssignedActivities = async () => {
    try {
        const userId = localStorage.getItem('userId');
        if (!userId) return [];

        const barberResponse = await api.get(`/barbers/${userId}`);
        const shopId = barberResponse.data.barbershopId;
        if (!shopId) return [];

        // Retorna todas as atividades da barbearia
        // TODO: Quando o backend tiver endpoint de atividades por barbeiro, usar ele
        const response = await api.get(`/barbershops/${shopId}/activities`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar minhas atividades:", error);
        return [];
    }
};

// Vincula atividades ao perfil do Barbeiro
// TODO: Esse endpoint precisa ser criado no backend (POST /api/barbers/me/assign-activities)
// Por enquanto mantém a chamada para quando o endpoint for implementado
export const assignActivities = async (activityIds) => {
    try {
        const response = await api.post('/barbers/me/assign-activities', { activityIds });
        return response.data;
    } catch (error) {
        console.error("Erro ao vincular atividades:", error);
        throw error;
    }
};