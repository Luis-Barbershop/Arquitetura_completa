import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { logoutUser } from '../services/authService';
import BarberHeader from '../components/BarberPage/BarberHeader';
import NoBarbershopPanel from '../components/BarberPage/NoBarbershopPanel';
import ManageServices from '../components/BarberPage/ManageServices';
import ManageMySkills from '../components/BarberPage/ManageMySkills';
import styles from './CSS/BarberHomePage.module.css';
import Invoicing from '../components/BarberPage/Invoicing';
import Buttonsbarber from '../components/BarberPage/Buttonsbarber';
import NextScheduling from '../components/BarberPage/NextScheduling';

function BarberHomePage() {
  const [barber, setBarber] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const userId = localStorage.getItem('userId');
    if (!userId) { navigate('/identificacao'); return; }

    api.get(`/barbers/${userId}`)
      .then(response => {
        setBarber(response.data);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
        if (err.response?.status === 403) navigate('/login');
      });
  }, [navigate]);

  const handleCreateShop = () => navigate('/create-barbershop');

  const handleJoinShop = () => {
    const cnpj = prompt("Digite o CNPJ da barbearia:");
    if (cnpj) {
      api.post('/barbershops/join-request', { cnpj })
        .then(() => alert("Pedido enviado! Aguarde o dono aceitar."))
        .catch(() => alert("Erro. Verifique o CNPJ."));
    }
  };

  const handleLogout = () => {
    if (window.confirm("Tem certeza que deseja sair?")) {
      logoutUser();
    }
  };

  if (loading) return <div className={styles.loadingContainer}>Carregando...</div>;

  return (
    <div className={styles.pageContainer}>
      <div className={styles.contentWrapper}>
        <BarberHeader barber={barber} onLogout={handleLogout} />

        {!barber?.barbershopId ? (
          <NoBarbershopPanel
            onCreateShop={handleCreateShop}
            onJoinShop={handleJoinShop}
          />
        ) : (
            <>
            <Invoicing />
            <Buttonsbarber />
            <NextScheduling/>
            </>


        //   <div>
        //     <button
        //       onClick={() => navigate('/meus-agendamentos')}
        //       className={styles.agendaButton}
        //     >
        //       Ver Minha Agenda
        //     </button>

        //     <ManageMySkills shopId={barber.barbershopId} />

        //     {barber.owner && <ManageServices />}
        //   </div>
        )}
      </div>
    </div>
  );
}

export default BarberHomePage;