import api from './api';

// Login Genérico (aceita 'customer' ou 'barber')
export const loginUser = async (email, password, userType = 'customer') => {
    let url = '';
    
    // 1. Define a URL correta baseada no Back-end (CustomerController ou BarberController)
    if (userType === 'barber') {
        url = '/barbers/login'; 
    } else {
        url = '/customers/login'; 
    }

    const response = await api.post(url, { email, password });
    
    // 2. Salva os dados se o login der certo
    if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        
        // 3. CORREÇÃO CRÍTICA: O Back-end não manda a 'role' explícita no DTO.
        // Precisamos definir manualmente para o appointmentService funcionar.
        const roleName = userType === 'barber' ? 'ROLE_BARBER' : 'ROLE_CUSTOMER';
        localStorage.setItem('role', roleName); 

        // O userData vem dentro de response.data
        if (response.data.userData) {
             localStorage.setItem('userName', response.data.userData.name);
             localStorage.setItem('userId', response.data.userData.id);
             // Salva o objeto completo caso precise depois
             localStorage.setItem('user', JSON.stringify(response.data.userData));
        }
    }
    
    // Retorna o objeto combinado para a tela usar
    return { ...response.data, role: localStorage.getItem('role') };
};

// Função de Cadastro (Cliente)
export const registerCustomer = async (userData) => {
    const formData = new FormData();

    // 1. Limpeza de caracteres especiais (CPF e Telefone)
    const cleanCPF = userData.documentCPF ? userData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = userData.tell ? userData.tell.replace(/\D/g, '') : '';

    // 2. Monta o objeto JSON
    const customerJson = JSON.stringify({
        name: userData.name,
        email: userData.email,
        password: userData.password,
        documentCPF: cleanCPF,
        tell: cleanTell
    });

    // 3. Adiciona o JSON como Blob (Obrigatório para @RequestPart do Java)
    const jsonBlob = new Blob([customerJson], { type: 'application/json' });
    formData.append('customer', jsonBlob);

    // 4. Se tiver arquivo de foto no futuro, adicione aqui:
    // if (userData.file) formData.append('file', userData.file);

    // 5. Envia para o endpoint correto
    const response = await api.post('/customers/register', formData);
    return response.data;
};

// Função de Logout
export const logoutUser = () => {
    localStorage.clear();
};

export const registerBarber = async (barberData) => {
    const formData = new FormData();
    
    // 1. Limpeza de caracteres especiais (CPF e Telefone)
    // O backend usa @CPF e @Size(max=11), então espera apenas números.
    const cleanCPF = barberData.documentCPF ? barberData.documentCPF.replace(/\D/g, '') : '';
    const cleanTell = barberData.tell ? barberData.tell.replace(/\D/g, '') : '';

    // 2. Monta o objeto JSON
    // NOTA: Removemos a função formatTime que adicionava ":00".
    // O HTML input type="time" já retorna "09:00", que é exatamente o que o teu @JsonFormat pede.
    const barberJson = JSON.stringify({
        name: barberData.name,
        email: barberData.email,
        password: barberData.password,
        documentCPF: cleanCPF,       
        tell: cleanTell,             
        workStartTime: barberData.workStartTime, // Envia "09:00"
        workEndTime: barberData.workEndTime      // Envia "18:00"
    });

    // 3. Adiciona o JSON como Blob com o Content-Type EXPLICITO de application/json
    // Isso é obrigatório para o @RequestPart("barber") do Java funcionar.
    const jsonBlob = new Blob([barberJson], { type: 'application/json' });
    formData.append('barber', jsonBlob);

    // Se tiver arquivo de foto no futuro, adicionas aqui:
    // if (barberData.file) formData.append('file', barberData.file);

    // 4. Envia SEM o header Content-Type manual (o axios gera o boundary sozinho)
    const response = await api.post('/barbers/register', formData);

    return response.data;
};