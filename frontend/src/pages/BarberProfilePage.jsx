import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import BarberNavbar from '../components/BarberPage/BarberNavbar';

/**
 * Página de Perfil do Barbeiro — exibe e permite editar dados pessoais.
 */
function BarberProfilePage() {
    const navigate = useNavigate();
    const [barber, setBarber] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            navigate('/identificacao', { state: { mode: 'login', role: 'barber' } });
            return;
        }
        api.get('/auth/me')
            .then(res => { setBarber(res.data); setLoading(false); })
            .catch(() => { setLoading(false); navigate('/identificacao'); });
    }, [navigate]);

    const handleLogout = async () => {
        await logoutUser();
        navigate('/');
    };

    if (loading) return <p style={{ padding: 32, color: '#fff' }}>Carregando perfil...</p>;

    return (
        <div style={{ minHeight: '100vh', background: '#0f0f1a', color: '#fff' }}>
            <BarberHeader
                barber={barber}
                onLogout={handleLogout}
                activeTab="perfil"
                onTabChange={(tab) => {
                    if (tab === 'home') navigate('/barberHome');
                    else if (tab === 'agenda') navigate('/meus-agendamentos');
                    else if (tab === 'servicos') navigate('/barberHome/servicos');
                    else if (tab === 'estoque') navigate('/barberHome/estoque');
                    else if (tab === 'time') navigate('/barberHome/time');
                    else if (tab === 'dashboards') navigate('/barberHome/dashboard');
                }}
            />
            <main style={{ maxWidth: 600, margin: '40px auto', padding: '0 16px' }}>
                <h2 style={{ marginBottom: 24 }}>Meu Perfil</h2>
                {barber && (
                    <div style={{
                        background: 'rgba(255,255,255,0.05)', borderRadius: 12,
                        padding: 24, display: 'flex', flexDirection: 'column', gap: 12
                    }}>
                        {barber.imageUrl && (
                            <img
                                src={barber.imageUrl}
                                alt="Foto de perfil"
                                style={{ width: 80, height: 80, borderRadius: '50%', objectFit: 'cover' }}
                            />
                        )}
                        <div><strong>Nome:</strong> {barber.name}</div>
                        <div><strong>E-mail:</strong> {barber.email}</div>
                        <div><strong>Telefone:</strong> {barber.tell || '—'}</div>
                        <div><strong>CPF:</strong> {barber.documentCPF || '—'}</div>
                        <div><strong>Barbearia:</strong> {barber.barbershopId ? `Vinculado (ID: ${barber.barbershopId})` : 'Sem barbearia'}</div>
                        <div><strong>Função:</strong> {barber.isOwner ? 'Dono do estabelecimento' : 'Colaborador'}</div>
                        <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', marginTop: 8 }}>
                            Para atualizar sua foto de perfil, use o botão de upload disponível nas configurações.
                        </p>
                    </div>
                )}
            </main>
            <BarberNavbar activeTab="perfil" onTabChange={() => {}} />
        </div>
    );
}

export default BarberProfilePage;
