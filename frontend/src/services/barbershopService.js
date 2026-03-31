import api from "./api"

export const getAllBarbershops = async () => {
    try {
        const response = await api.get("/barbershops");
        return response.data;
    } catch (error) {
        console.log("Erro de API - API AllBarbershops");
        return[];
    };
}

export const getBarbershopById = async (id) => {
    try {
        const response = await api.get("/barbershops/")
    } catch (error) {
         console.log("Erro de API - API GetBarbershopById");
        return[];
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
    try {
        const response = await api.get(`/barbershops/${shopId}/barbers`);
        return response.data;
    } catch (error) {
        console.error("Erro ao buscar barbeiros da loja:", error);
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


export const getMyServices = async () => {
    // Precisamos primeiro saber o ID da loja do usuário logado
    // Uma forma segura é buscar os dados do barbeiro primeiro
    try {
        const meResponse = await api.get('/barbers/me');
        const shopId = meResponse.data.barbershopId;
        
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