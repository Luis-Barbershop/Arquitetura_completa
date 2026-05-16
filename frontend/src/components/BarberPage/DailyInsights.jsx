import { useEffect, useMemo, useState } from 'react';
import api from '../../services/api';
import Styles from "./CSS/DailyInsights.module.css"

function DailyInsights({ barbershopId }) {
  const [todayAppointmentsCount, setTodayAppointmentsCount] = useState(0);
  const [lowStockCount, setLowStockCount] = useState(0);

  useEffect(() => {
    const loadInsights = async () => {
      try {
        const appointmentsResponse = await api.get('/appointments/my-appointments');
        const appointments = Array.isArray(appointmentsResponse.data) ? appointmentsResponse.data : [];
        const today = new Date();
        const isSameDay = (date) => (
          date.getFullYear() === today.getFullYear()
          && date.getMonth() === today.getMonth()
          && date.getDate() === today.getDate()
        );

        const activeToday = appointments.filter((item) => {
          const start = item?.startTime ? new Date(item.startTime) : null;
          if (!start || Number.isNaN(start.getTime())) return false;
          const status = String(item?.status || '').toUpperCase();
          const activeStatuses = ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'WALK_IN', 'COMPLETED', 'CONCLUDED'];
          return isSameDay(start) && activeStatuses.includes(status);
        });

        setTodayAppointmentsCount(activeToday.length);
      } catch (error) {
        console.error('Erro ao carregar agenda para insights:', error);
      }

      if (!barbershopId) return;

      try {
        const lowStockResponse = await api.get('/products/inventory', {
          params: {
            barbershopId,
            lowStock: true,
            page: 0,
            size: 1,
          },
        });
        setLowStockCount(Number(lowStockResponse.data?.total ?? 0));
      } catch (error) {
        console.error('Erro ao carregar estoque para insights:', error);
      }
    };

    loadInsights();
  }, [barbershopId]);

  const occupancyPct = useMemo(() => {
    const maxAppointmentsPerDay = 16;
    return Math.min(100, Math.round((todayAppointmentsCount / maxAppointmentsPerDay) * 100));
  }, [todayAppointmentsCount]);

  const insightText = useMemo(() => {
    if (lowStockCount > 0) {
      return `Voce tem ${lowStockCount} item(ns) com estoque baixo. Planeje reposicao para evitar ruptura nos atendimentos.`;
    }
    if (occupancyPct >= 85) {
      return `Agenda forte hoje: ocupacao em ${occupancyPct}%. Priorize pontualidade e confirme os proximos atendimentos.`;
    }
    return `Agenda de hoje em ${occupancyPct}%. Ainda ha espaco para encaixes e divulgacao de horarios vagos.`;
  }, [lowStockCount, occupancyPct]);

  const highlight = useMemo(() => {
    if (lowStockCount > 0) {
      return `${lowStockCount} ALERTA(S) DE ESTOQUE`;
    }
    return `OCUPACAO HOJE: ${occupancyPct}%`;
  }, [lowStockCount, occupancyPct]);

  return (
    <div className={Styles.container}>
        <div className={Styles.headerDailyInsights}>
            <div className={Styles.dailyInsightIcon}>
                <img src="/Icons/dailyInsights.png" alt="Ícone de Insights" />
            </div>

            <div>
            <h4 className={Styles.title}>Insights Diários</h4>
            <p>INTELIGÊNCIA CORTA AÍ</p>
            </div>

        </div>

        <div className={Styles.content}>
            <p>{insightText}</p>
        </div>

        <div className={Styles.footer}>
            <p>{highlight}</p>
            <button className={Styles.seeMoreButton}>Atualizar dados</button>
        </div>

    </div>
  )
}

export default DailyInsights
