/**
 * userContext.js — Fonte única de verdade para o contexto de usuário logado.
 *
 * Lê `userRole` e `isOwner` do localStorage (gravados em authService.js
 * após login / verify). Não faz requisição à API — apenas lê cache local.
 *
 * Regras de acesso:
 *   ROLE_CUSTOMER  → homepage do cliente (/homepage), meus agendamentos como cliente
 *   ROLE_BARBER    → painel do barbeiro (/barberHome), agenda como profissional
 *     isOwner=true  → acesso a Dashboard, Estoque, Equipe (Time), Gerenciar Serviços
 *     isOwner=false → acesso somente a Home, Agenda, Meus Serviços, Perfil
 */

/** Retorna o role armazenado no localStorage, ou null se não logado. */
export const getUserRole = () => localStorage.getItem('userRole') || null;

/** Retorna true se o usuário logado é cliente. */
export const isCustomer = () => {
    const role = getUserRole();
    return role === 'ROLE_CUSTOMER';
};

/** Retorna true se o usuário logado é barbeiro (colaborador ou owner). */
export const isBarber = () => {
    const role = getUserRole();
    return role === 'ROLE_BARBER' || role === 'ROLE_OWNER';
};

/**
 * Retorna true se o barbeiro logado é dono do estabelecimento.
 * Combina o flag `isOwner` do localStorage com o role.
 */
export const isOwnerUser = () => {
    const role = getUserRole();
    const ownerFlag = localStorage.getItem('isOwner');
    return (
        role === 'ROLE_OWNER' ||
        ownerFlag === 'true' ||
        String(role || '').toUpperCase().includes('OWNER')
    );
};

/** Retorna true se o usuário está logado (tem token). */
export const isLoggedIn = () => !!localStorage.getItem('token');

/**
 * Retorna o barbershopId do barbeiro logado (salvo no localStorage após login/verify).
 * Retorna null se não houver barbearia vinculada ou se o usuário for cliente.
 */
export const getBarbershopId = () => localStorage.getItem('barbershopId') || null;

/**
 * Retorna o objeto de acesso do barbeiro:
 *   { canManageTeam, canManageStock, canViewDashboard, canManageShopServices }
 * Para clientes, todos os campos são false.
 */
export const getBarberAccess = () => {
    const owner = isOwnerUser();
    return {
        canManageTeam: owner,
        canManageStock: owner,
        canViewDashboard: owner,
        canManageShopServices: owner,
    };
};
